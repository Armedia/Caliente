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
public class CmsImporter extends CmsTransferEngine {

	public CmsImporter() {
		super();
	}

	public CmsImporter(int threadCount) {
		super(threadCount);
	}

	public CmsImporter(int threadCount, int backlogSize) {
		super(threadCount, backlogSize);
	}

	public void doImport(final CmsObjectStore objectStore, final DctmSessionManager sessionManager) throws DfException,
	CMSMFException {

		final int threadCount = getThreadCount();
		final int backlogSize = getBacklogSize();
		final SynchronizedCounter waitCounter = new SynchronizedCounter();
		final AtomicInteger activeCounter = new AtomicInteger(0);
		final CmsObject<?> exitValue = new CmsUser();
		final BlockingQueue<CmsObject<?>> workQueue = new ArrayBlockingQueue<CmsObject<?>>(threadCount * backlogSize);
		final ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
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

						if (next == exitValue) {
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
		for (int i = 0; i < threadCount; i++) {
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

			// Ask the workers to exit civilly
			this.log.info("Signaling work completion for the workers");
			for (int i = 0; i < threadCount; i++) {
				try {
					workQueue.put(exitValue);
				} catch (InterruptedException e) {
					// Here we have a problem: we're timing out while adding the exit values...
					this.log.warn("Interrupted while attempting to request executor thread termination", e);
					break;
				}
			}

			// If there are still pending workers, then wait for them to finish for up to 5
			// minutes
			int pending = activeCounter.get();
			if (pending > 0) {
				this.log.info(String.format(
					"Waiting for pending workers to terminate (maximum 5 minutes, %d pending workers)", pending));
				try {
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