/**
 *
 */

package com.delta.cmsmf.engine;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cms.CmsCounter;
import com.delta.cmsmf.cms.CmsCounter.Result;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.CmsUser;
import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.delta.cmsmf.cms.storage.CmsObjectStore.ObjectHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsImporter {

	public static class SynchronizedCounter {
		private long value;

		public SynchronizedCounter() {
			this(0);
		}

		public SynchronizedCounter(long initialValue) {
			this.value = initialValue;
		}

		public synchronized long decrement() {
			return increment(-1);
		}

		public synchronized long decrement(long amount) {
			return increment(-amount);
		}

		public synchronized long increment() {
			return increment(1);
		}

		public synchronized long increment(long amount) {
			this.value += amount;
			notify();
			return this.value;
		}

		public synchronized long setValue(long value) {
			long prev = this.value;
			this.value = value;
			notify();
			return prev;
		}

		public synchronized long getValue() {
			return this.value;
		}

		public synchronized void waitUntilChange() throws InterruptedException {
			final long current = this.value;
			while (this.value == current) {
				wait();
			}
			notify();
		}

		public synchronized void waitForValue(long value) throws InterruptedException {
			while (this.value != value) {
				wait();
			}
			notify();
		}
	}

	private Logger log = Logger.getLogger(getClass());

	public static final int DEFAULT_BACKLOG_SIZE = 100;
	public static final int MAX_BACKLOG_SIZE = 1000;
	public static final int DEFAULT_THREAD_COUNT = 4;
	public static final int MAX_THREAD_COUNT = 32;

	private final int backlogSize;
	private final int threadCount;

	public CmsImporter() {
		this(CmsImporter.DEFAULT_THREAD_COUNT);
	}

	public CmsImporter(int threadCount) {
		this(threadCount, CmsImporter.DEFAULT_BACKLOG_SIZE);
	}

	public CmsImporter(int threadCount, int backlogSize) {
		if (threadCount <= 0) {
			threadCount = 1;
		}
		if (threadCount > CmsImporter.MAX_THREAD_COUNT) {
			threadCount = CmsImporter.MAX_THREAD_COUNT;
		}
		if (backlogSize <= 0) {
			backlogSize = 10;
		}
		if (backlogSize > CmsImporter.MAX_BACKLOG_SIZE) {
			backlogSize = CmsImporter.MAX_BACKLOG_SIZE;
		}
		this.threadCount = threadCount;
		this.backlogSize = backlogSize;
	}

	public int getBacklogSize() {
		return this.backlogSize;
	}

	public int getThreadCount() {
		return this.threadCount;
	}

	public void doImport(final CmsObjectStore objectStore, final DctmSessionManager sessionManager) throws DfException,
		CMSMFException {

		final SynchronizedCounter waitCounter = new SynchronizedCounter();
		final AtomicInteger activeCounter = new AtomicInteger(0);
		final CmsObject<?> exitFlag = new CmsUser();
		final BlockingQueue<CmsObject<?>> workQueue = new ArrayBlockingQueue<CmsObject<?>>(this.threadCount
			* this.backlogSize);
		ExecutorService executor = new ThreadPoolExecutor(this.threadCount, this.threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				IDfSession session = sessionManager.acquireSession();
				if (CmsImporter.this.log.isDebugEnabled()) {
					CmsImporter.this.log.debug(String.format("Got IDfSession [%s]", DfUtils.getSessionId(session)));
				}
				activeCounter.incrementAndGet();
				try {
					while (true) {
						if (CmsImporter.this.log.isDebugEnabled()) {
							CmsImporter.this.log.debug("Polling the queue...");
						}
						final CmsObject<?> next;
						// increase the waiter count
						waitCounter.increment();
						try {
							next = workQueue.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						} finally {
							// decrease the waiter count
							waitCounter.decrement();
						}

						if (next == exitFlag) {
							CmsImporter.this.log.info("Exiting the export polling loop");
							return;
						}

						if (CmsImporter.this.log.isDebugEnabled()) {
							CmsImporter.this.log.debug(String.format("Polled %s", next));
						}

						Result result = Result.FAILED;
						try {
							result = next.saveToCMS(session, objectStore);
							if (CmsImporter.this.log.isDebugEnabled()) {
								CmsImporter.this.log.debug(String.format("Persisted (%s) %s", result, next));
							}
						} catch (Throwable t) {
							result = Result.FAILED;
							// Log the error, move on
							CmsImporter.this.log.error(String.format("Exception caught processing %s", next), t);
						} finally {
							CmsCounter.incrementCounter(next, result);
						}
					}
				} finally {
					activeCounter.decrementAndGet();
					sessionManager.releaseSession(session);
				}
			}
		};

		// Fire off the workers
		for (int i = 0; i < this.threadCount; i++) {
			executor.submit(worker);
		}
		executor.shutdown();

		// 1: run the query for the given predicate
		try {
			final Map<CmsObjectType, Integer> containedTypes = objectStore.getStoredObjectTypes();
			final ObjectHandler handler = new ObjectHandler() {
				@Override
				public boolean handle(CmsObject<?> dataObject) {
					workQueue.add(dataObject);
					return true;
				}
			};

			outer: for (CmsObjectType type : CmsObjectType.values()) {
				final Integer total = containedTypes.get(type);
				if (total == null) {
					this.log.warn(String.format("No %s objects are contained in the export", type.name()));
					continue;
				}

				if (total < 1) {
					this.log.warn(String.format("No %s objects available"));
					continue;
				}

				this.log.info(String.format("%d %s objects available, starting deserialization", total, type.name()));
				objectStore.deserializeObjects(type, handler);

				// We're done, we must wait until all workers are waiting
				// This "weird" condition allows us to wait until there are
				// as many waiters as there are active threads. This covers
				// the contingency of threads dying on us, which SHOULDN'T
				// happen, but still, we defend against it.
				this.log.info(String.format(
					"Submitted the entire %s workload (%d objects), waiting for it to complete", type.name(), total));
				synchronized (waitCounter) {
					while (waitCounter.getValue() < activeCounter.get()) {
						try {
							waitCounter.wait();
							waitCounter.notify();
						} catch (InterruptedException e) {
							// We've been interrupted...that means that...what?
							this.log.fatal(String.format("Interrupted while waiting for the %s workload to complete",
								type));
							break outer;
						}
					}
				}
			}

			for (int i = 0; i < this.threadCount; i++) {
				try {
					workQueue.put(exitFlag);
				} catch (InterruptedException e) {
					// Here we have a problem: we're timing out while adding the exit values...
					this.log.warn("Interrupted while attempting to request executor thread termination", e);
					break;
				}
			}
			int pending = activeCounter.get();
			if (pending > 0) {
				try {
					this.log.info(String.format(
						"Waiting for pending workers to terminate (maximum 5 minutes, %d pending workers)", pending));
					executor.awaitTermination(5, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					this.log.warn("Interrupted while waiting for normal executor termination", e);
				}
			}
		} finally {
			executor.shutdownNow();
			int pending = activeCounter.get();
			if (pending > 0) {
				try {
					this.log
						.info(String
							.format(
								"Waiting an additional 60 seconds for worker termination as a contingency (%d pending workers)",
								pending));
					executor.awaitTermination(1, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					this.log.warn("Interrupted while waiting for immediate executor termination", e);
				}
			}
			for (CmsObjectType type : CmsObjectType.values()) {
				this.log.info(String.format("Action report for %s:%n%s", type.name(), CmsCounter.generateReport(type)));
			}
			this.log.info(String.format("Summary Report:%n%s", CmsCounter.generateCummulativeReport()));
		}
	}
}