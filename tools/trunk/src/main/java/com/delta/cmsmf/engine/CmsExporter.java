/**
 *
 */

package com.delta.cmsmf.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

import com.delta.cmsmf.cms.CmsExportResult;
import com.delta.cmsmf.cms.CmsFileSystem;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.DefaultTransferContext;
import com.delta.cmsmf.cms.DfValueFactory;
import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsExporter extends CmsTransferEngine<CmsExportEventListener> {

	public CmsExporter() {
		super();
	}

	public CmsExporter(int threadCount) {
		super(threadCount);
	}

	public CmsExporter(int threadCount, int backlogSize) {
		super(threadCount, backlogSize);
	}

	public void doExport(final CmsObjectStore objectStore, final DctmSessionManager sessionManager,
		final CmsFileSystem fileSystem, final String dqlPredicate) throws DfException, CMSMFException {

		final IDfSession session = sessionManager.acquireSession();

		final int threadCount = getThreadCount();
		final int backlogSize = getBacklogSize();
		final AtomicInteger activeCounter = new AtomicInteger(0);
		final IDfValue exitValue = DfValueFactory.newStringValue(String.format("%s[%s]", toString(), UUID.randomUUID()
			.toString()));
		final BlockingQueue<IDfValue> workQueue = new ArrayBlockingQueue<IDfValue>(threadCount * backlogSize);
		final ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				IDfSession session = sessionManager.acquireSession();
				boolean transOk = false;
				try {
					session.beginTrans();
					transOk = true;
				} catch (DfException e) {
					CmsExporter.this.log.warn("FAILED TO INITIALIZE A TRANSACTION FOR EXPORT. Will continue anyway", e);
					transOk = false;
				}
				activeCounter.incrementAndGet();
				try {
					while (!Thread.interrupted()) {
						if (CmsExporter.this.log.isDebugEnabled()) {
							CmsExporter.this.log.debug("Polling the queue...");
						}
						final IDfValue next;
						try {
							next = workQueue.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}

						if (next == exitValue) {
							// Work complete
							CmsExporter.this.log.info("Exiting the export polling loop");
							return;
						}

						final IDfId id = next.asId();
						CmsObjectType type = null;
						Throwable thrown = null;
						if (CmsExporter.this.log.isDebugEnabled()) {
							CmsExporter.this.log.debug(String.format("Polled ID %s", id.getId()));
						}

						if (CmsExporter.this.log.isDebugEnabled()) {
							CmsExporter.this.log.debug(String.format("Got IDfSession [%s]",
								DfUtils.getSessionId(session)));
						}
						CmsExportResult result = CmsExportResult.FAILED;
						try {
							IDfPersistentObject dfObj = session.getObject(id);
							if (CmsExporter.this.log.isDebugEnabled()) {
								CmsExporter.this.log.debug(String.format("Retrieved [%s] object with id [%s]", dfObj
									.getType().getName(), dfObj.getObjectId().getId()));
							}
							type = CmsObjectType.decodeType(dfObj);
							objectExportStarted(type, dqlPredicate);
							result = (objectStore.persistDfObject(dfObj, new DefaultTransferContext(dfObj.getObjectId()
								.getId(), session, objectStore, fileSystem)) ? CmsExportResult.EXPORTED
								: CmsExportResult.SKIPPED);
							if (CmsExporter.this.log.isDebugEnabled()) {
								CmsExporter.this.log.debug(String.format("Persisted [%s] object with id [%s]", dfObj
									.getType().getName(), dfObj.getObjectId().getId()));
							}
						} catch (Throwable t) {
							// Log the error, move on
							thrown = t;
							CmsExporter.this.log.error(
								String.format("Exception caught processing object with ID [%s]", id.getId()), t);
						} finally {
							objectExportFinished(type, id.getId(), result, thrown);
						}
					}
				} finally {
					activeCounter.decrementAndGet();
					if (transOk) {
						try {
							session.abortTrans();
						} catch (DfException e) {
							CmsExporter.this.log.warn("FAILED TO ABORT THE OPEN TRANSACTION.", e);
						}
					}
					sessionManager.releaseSession(session);
				}
			}
		};

		// Fire off the workers
		List<Future<?>> futures = new ArrayList<Future<?>>(threadCount);
		for (int i = 0; i < threadCount; i++) {
			futures.add(executor.submit(worker));
		}
		executor.shutdown();
		IDfCollection results = null;
		try {
			// 1: run the query for the given predicate
			final String dql = String.format("select distinct r_object_id %s", dqlPredicate);
			results = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			exportStarted(dql);
			// 2: iterate over the results, gathering up the object IDs
			int counter = 0;
			while (results.next()) {
				if (this.log.isTraceEnabled()) {
					this.log.trace("Retrieving the next ID from the query");
				}
				final IDfValue id = results.getValue("r_object_id");
				counter++;
				try {
					workQueue.put(id);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					if (this.log.isDebugEnabled()) {
						this.log.warn(String.format("Thread interrupted after reading %d object IDs", counter), e);
					} else {
						this.log.warn(String.format("Thread interrupted after reading %d objects IDs", counter));
					}
					break;
				}
			}

			try {
				// Ask the workers to exit civilly
				this.log.info("Signaling work completion for the workers");
				for (int i = 0; i < threadCount; i++) {
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

				// We're done, we must wait until all workers are waiting
				// This "weird" condition allows us to wait until there are
				// as many waiters as there are active threads. This covers
				// the contingency of threads dying on us, which SHOULDN'T
				// happen, but still, we defend against it.
				this.log.info(String.format(
					"Submitted the entire export workload (%d objects), waiting for it to complete", counter));
				for (Future<?> future : futures) {
					try {
						future.get();
					} catch (InterruptedException e) {
						this.log.warn("Interrupted while wiating for an executor thread to exit", e);
						Thread.currentThread().interrupt();
						executor.shutdownNow();
						break;
					} catch (ExecutionException e) {
						this.log.warn("An executor thread raised an exception", e);
					} catch (CancellationException e) {
						this.log.warn("An executor thread was canceled!", e);
					}
				}
				this.log.info("All the export workers are done.");
			} finally {
				List<IDfValue> remaining = new ArrayList<IDfValue>();
				workQueue.drainTo(remaining);
				for (IDfValue v : remaining) {
					if (v == exitValue) {
						continue;
					}
					this.log.fatal(String.format("WORK LEFT PENDING IN THE QUEUE: %s", v.asId().getId()));
				}
				remaining.clear();
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
					Thread.currentThread().interrupt();
				}
			}
		} finally {
			Map<CmsObjectType, Integer> summary = Collections.emptyMap();
			try {
				summary = objectStore.getStoredObjectTypes();
			} catch (CMSMFException e) {
				this.log.warn("Exception caught attempting to get the work summary", e);
			}
			exportConcluded(summary);

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
			DfUtils.closeQuietly(results);
			sessionManager.releaseSession(session);
		}
	}

	private void exportStarted(String dql) {
		for (CmsExportEventListener l : getListeners()) {
			try {
				l.exportStarted(dql);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectExportStarted(CmsObjectType objectType, String objectId) {
		for (CmsExportEventListener l : getListeners()) {
			try {
				l.objectExportStarted(objectType, objectId);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectExportFinished(CmsObjectType objectType, String objectId, CmsExportResult result,
		Throwable thrown) {
		for (CmsExportEventListener l : getListeners()) {
			try {
				l.objectExportFinished(objectType, objectId, result, thrown);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void exportConcluded(Map<CmsObjectType, Integer> summary) {
		for (CmsExportEventListener l : getListeners()) {
			try {
				l.exportConcluded(summary);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}
}