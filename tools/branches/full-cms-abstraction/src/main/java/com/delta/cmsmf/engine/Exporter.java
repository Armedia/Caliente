/**
 *
 */

package com.delta.cmsmf.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.slf4j.Logger;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DctmTranslator;
import com.armedia.cmf.documentum.engine.DfUtils;
import com.armedia.cmf.documentum.engine.DfValueFactory;
import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.cmf.engine.exporter.ExportListener;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.exception.CMSMFException;
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
public class Exporter extends TransferEngine<ExportEngineListener> {

	private final Set<String> noiseTracker = Collections.synchronizedSet(new HashSet<String>());

	private final ExportListener exportListener = new ExportListener() {

		@Override
		public void objectSkipped(StoredObjectType objectType, String objectId) {
			Exporter.this.objectSkipped(objectType, objectId);
		}

		@Override
		public void objectExportStarted(StoredObjectType objectType, String objectId) {
			Exporter.this.objectExportStarted(objectType, objectId);
		}

		@Override
		public void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown) {
			Exporter.this.objectExportFailed(objectType, objectId, thrown);
		}

		@Override
		public void objectExportCompleted(StoredObject<?> object, Long objectNumber) {
			Exporter.this.objectExportCompleted(object, objectNumber);
		}
	};

	public Exporter(ObjectStore objectStore, ContentStore fileSystem) {
		super(objectStore, fileSystem);
	}

	public Exporter(ObjectStore objectStore, ContentStore fileSystem, int threadCount) {
		super(objectStore, fileSystem, threadCount);
	}

	public Exporter(ObjectStore objectStore, ContentStore fileSystem, int threadCount, int backlogSize) {
		super(objectStore, fileSystem, threadCount, backlogSize);
	}

	public Exporter(ObjectStore objectStore, ContentStore fileSystem, Logger output) {
		super(objectStore, fileSystem, output);
	}

	public Exporter(ObjectStore objectStore, ContentStore fileSystem, Logger output, int threadCount) {
		super(objectStore, fileSystem, output, threadCount);
	}

	public Exporter(ObjectStore objectStore, ContentStore fileSystem, Logger output, int threadCount,
		int backlogSize) {
		super(objectStore, fileSystem, output, threadCount, backlogSize);
	}

	private boolean isTracked(StoredObjectType objectType, String objectId) {
		boolean ret = false;
		try {
			ret = getObjectStore().isStored(objectType, objectId);
		} catch (StorageException e) {
			if (this.log.isDebugEnabled()) {
				this.log.error(
					String.format("Failed to check whether %s[%s] was serialized", objectType.name(), objectId), e);
			}
			ret = false;
		}
		if (!ret) {
			// Some objects aren't persisted, but should still be tracked...
			String key = String.format("%s[%s]", objectType.name(), objectId);
			ret = !this.noiseTracker.add(key);
		}
		return ret;
	}

	private boolean isTracked(StoredObject<?> object) {
		return isTracked(object.getType(), object.getId());
	}

	public void doExport(final DctmSessionManager sessionManager, final String dqlPredicate) throws DfException,
		CMSMFException {
		final IDfSession session = sessionManager.acquireSession();
		final ObjectStore objectStore = getObjectStore();
		final ContentStore fileSystem = getContentStreamStore();

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
				final IDfSession session = sessionManager.acquireSession();
				final Logger output = getOutput();
				boolean transOk = false;
				try {
					session.beginTrans();
					transOk = true;
				} catch (DfException e) {
					Exporter.this.log.warn("FAILED TO INITIALIZE A TRANSACTION FOR EXPORT. Will continue anyway", e);
					transOk = false;
				}
				activeCounter.incrementAndGet();
				try {
					while (!Thread.interrupted()) {
						if (Exporter.this.log.isDebugEnabled()) {
							Exporter.this.log.debug("Polling the queue...");
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
							Exporter.this.log.info("Exiting the export polling loop");
							return;
						}

						final IDfId id = next.asId();
						DctmObjectType type = null;
						Throwable thrown = null;
						if (Exporter.this.log.isDebugEnabled()) {
							Exporter.this.log.debug(String.format("Polled ID %s", id.getId()));
						}

						if (Exporter.this.log.isDebugEnabled()) {
							Exporter.this.log
								.debug(String.format("Got IDfSession [%s]", DfUtils.getSessionId(session)));
						}
						try {
							IDfPersistentObject dfObj = session.getObject(id);
							if (Exporter.this.log.isDebugEnabled()) {
								Exporter.this.log.debug(String.format("Retrieved [%s] object with id [%s]", dfObj
									.getType().getName(), dfObj.getObjectId().getId()));
							}
							type = DctmObjectType.decodeType(dfObj);
							final String objectId = dfObj.getObjectId().getId();

							objectExportStarted(type.getStoredObjectType(), objectId);
							StoredObject<IDfValue> object = null; // convert from dfObj
							DctmExportContext ctx = new DctmExportContext(objectId, session, objectStore, fileSystem,
								output, Exporter.this.exportListener);
							Long objectNumber = objectStore.storeObject(object, DctmTranslator.INSTANCE);
							if (objectNumber != null) {
								objectExportCompleted(object, objectNumber);
							} else {
								objectSkipped(type.getStoredObjectType(), objectId);
							}
							if (Exporter.this.log.isDebugEnabled()) {
								Exporter.this.log.debug(String.format("Persisted [%s] object with id [%s]", dfObj
									.getType().getName(), objectId));
							}
						} catch (Throwable t) {
							// Log the error, move on
							objectExportFailed(type.getStoredObjectType(), id.getId(), thrown);
							thrown = t;
							Exporter.this.log.error(
								String.format("Exception caught processing object with ID [%s]", id.getId()), t);
						} finally {
						}
					}
				} finally {
					activeCounter.decrementAndGet();
					if (transOk) {
						try {
							session.abortTrans();
						} catch (DfException e) {
							Exporter.this.log.warn("FAILED TO ABORT THE OPEN TRANSACTION.", e);
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
					this.log.error(String.format("WORK LEFT PENDING IN THE QUEUE: %s", v.asId().getId()));
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
			Map<StoredObjectType, Integer> summary = Collections.emptyMap();
			try {
				summary = objectStore.getStoredObjectTypes();
			} catch (StorageException e) {
				this.log.warn("Exception caught attempting to get the work summary", e);
			}
			exportFinished(summary);

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
		for (ExportEngineListener l : getListeners()) {
			try {
				l.exportStarted(null);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectExportStarted(StoredObjectType objectType, String objectId) {
		if (isTracked(objectType, objectId)) { return; }
		for (ExportEngineListener l : getListeners()) {
			try {
				l.objectExportStarted(objectType, objectId);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectExportCompleted(StoredObject<?> object, Long objectNumber) {
		isTracked(object);
		for (ExportEngineListener l : getListeners()) {
			try {
				l.objectExportCompleted(object, objectNumber);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectSkipped(StoredObjectType objectType, String objectId) {
		if (isTracked(objectType, objectId)) { return; }
		for (ExportEngineListener l : getListeners()) {
			try {
				l.objectSkipped(objectType, objectId);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown) {
		if (isTracked(objectType, objectId)) { return; }
		for (ExportEngineListener l : getListeners()) {
			try {
				l.objectExportFailed(objectType, objectId, thrown);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void exportFinished(Map<StoredObjectType, Integer> summary) {
		for (ExportEngineListener l : getListeners()) {
			try {
				l.exportFinished(summary);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}
}