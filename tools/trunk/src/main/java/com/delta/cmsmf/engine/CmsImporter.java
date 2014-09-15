/**
 *
 */

package com.delta.cmsmf.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.delta.cmsmf.cms.CmsCounter;
import com.delta.cmsmf.cms.CmsDependencyType;
import com.delta.cmsmf.cms.CmsImportResult;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObject.SaveResult;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.CmsTransferContext;
import com.delta.cmsmf.cms.CmsUser;
import com.delta.cmsmf.cms.DefaultTransferContext;
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

	private final CmsCounter<CmsImportResult> counter = new CmsCounter<CmsImportResult>(CmsImportResult.class);

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
		final AtomicInteger activeCounter = new AtomicInteger(0);
		final CmsObject<?> exitValue = new CmsUser();
		final BlockingQueue<CmsObject<?>> workQueue = new ArrayBlockingQueue<CmsObject<?>>(threadCount * backlogSize);
		final ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		this.counter.reset();

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				IDfSession session = sessionManager.acquireSession();
				if (CmsImporter.this.log.isDebugEnabled()) {
					CmsImporter.this.log.debug(String.format("Got IDfSession [%s]", DfUtils.getSessionId(session)));
				}
				activeCounter.incrementAndGet();
				try {
					while (!Thread.interrupted()) {
						if (CmsImporter.this.log.isDebugEnabled()) {
							CmsImporter.this.log.debug("Polling the queue...");
						}
						final CmsObject<?> next;
						// increase the waiter count
						try {
							next = workQueue.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}

						if (next == exitValue) {
							CmsImporter.this.log.info("Exiting the export polling loop");
							return;
						}

						if (CmsImporter.this.log.isDebugEnabled()) {
							CmsImporter.this.log.debug(String.format("Polled %s", next));
						}
						CmsTransferContext ctx = new DefaultTransferContext(next.getId(), session, objectStore);
						SaveResult result = null;
						try {
							result = next.saveToCMS(ctx);
							if (CmsImporter.this.log.isDebugEnabled()) {
								CmsImporter.this.log.debug(String.format("Persisted (%s) %s", result, next));
							}
						} catch (Throwable t) {
							result = null;
							// Log the error, move on
							CmsImporter.this.log.error(String.format("Exception caught processing %s", next), t);
						} finally {
							CmsImporter.this.counter.increment(next, (result != null ? result.getResult()
								: CmsImportResult.FAILED));
						}
					}
				} finally {
					CmsImporter.this.log.debug("Worker exiting...");
					activeCounter.decrementAndGet();
					sessionManager.releaseSession(session);
				}
			}
		};

		// 1: run the query for the given predicate
		try {
			final Map<CmsObjectType, Integer> containedTypes = objectStore.getStoredObjectTypes();
			final ObjectHandler handler = new ObjectHandler() {
				@Override
				public boolean handle(CmsObject<?> dataObject) {
					try {
						workQueue.put(dataObject);
						return true;
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						if (CmsImporter.this.log.isDebugEnabled()) {
							CmsImporter.this.log
							.warn(String.format("Thread interrupted while trying to submit the object %s",
								dataObject), e);
						} else {
							CmsImporter.this.log.warn(String.format(
								"Thread interrupted while trying to submit the object %s", dataObject));
						}
						return false;
					}
				}
			};

			List<Future<?>> futures = new ArrayList<Future<?>>();
			List<CmsObject<?>> remaining = new ArrayList<CmsObject<?>>();
			for (CmsObjectType type : CmsObjectType.values()) {
				final Integer total = containedTypes.get(type);
				if (total == null) {
					this.log.warn(String.format("No %s objects are contained in the export", type.name()));
					continue;
				}

				if (total < 1) {
					this.log.warn(String.format("No %s objects available"));
					continue;
				}

				// Start the workers
				futures.clear();
				final int workerCount = (type.getPeerDependencyType() == CmsDependencyType.HIERARCHY ? 1 : threadCount);
				for (int i = 0; i < workerCount; i++) {
					futures.add(executor.submit(worker));
				}

				this.log.info(String.format("%d %s objects available, starting deserialization", total, type.name()));
				objectStore.deserializeObjects(type, handler);

				try {
					// Ask the workers to exit civilly after the entire workload is submitted
					this.log.info(String.format("Signaling work completion for the %s workers", type.name()));
					for (int i = 0; i < workerCount; i++) {
						try {
							workQueue.put(exitValue);
						} catch (InterruptedException e) {
							// Here we have a problem: we're timing out while adding the exit
							// values...
							this.log.warn("Interrupted while attempting to request executor thread termination", e);
							Thread.currentThread().interrupt();
							break;
						}
					}

					// Here, we wait for all the workers to conclude
					this.log.info(String.format("Waiting for the %s workers to exit...", type.name()));
					for (Future<?> future : futures) {
						try {
							future.get();
						} catch (InterruptedException e) {
							this.log.warn("Interrupted while wiating for an executor thread to exit", e);
							Thread.currentThread().interrupt();
							break;
						} catch (ExecutionException e) {
							this.log.warn("An executor thread raised an exception", e);
						} catch (CancellationException e) {
							this.log.warn("An executor thread was canceled!", e);
						}
					}
					this.log.info(String.format("All the %s workers are done, continuing with the next object type...",
						type.name()));
				} finally {
					workQueue.drainTo(remaining);
					for (CmsObject<?> v : remaining) {
						if (v == exitValue) {
							continue;
						}
						this.log.fatal(String.format("WORK LEFT PENDING IN THE QUEUE: %s", v));
					}
					remaining.clear();
				}
			}

			// Shut down the executor
			executor.shutdown();

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
					Thread.currentThread().interrupt();
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
					Thread.currentThread().interrupt();
				}
			}
			for (CmsObjectType type : CmsObjectType.values()) {
				this.log
					.info(String.format("Action report for %s:%n%s", type.name(), this.counter.generateReport(type)));
			}
			this.log.info(String.format("Summary Report:%n%s", this.counter.generateCummulativeReport()));
		}
	}
}