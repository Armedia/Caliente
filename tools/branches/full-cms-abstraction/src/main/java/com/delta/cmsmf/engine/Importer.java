/**
 *
 */

package com.delta.cmsmf.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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

import org.apache.log4j.Logger;

import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectCounter;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.delta.cmsmf.cms.DctmDependencyType;
import com.delta.cmsmf.cms.DctmTransferContext;
import com.delta.cmsmf.cms.DctmObjectType;
import com.delta.cmsmf.cms.DctmPersistentObject.SaveResult;
import com.delta.cmsmf.cms.DctmTranslator;
import com.delta.cmsmf.cms.DefaultTransferContext;
import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.CMSMFUtils;
import com.delta.cmsmf.utils.DfUtils;
import com.delta.cmsmf.utils.SynchronizedCounter;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class Importer extends TransferEngine<ImportEngineListener> {

	private final StoredObjectCounter<StoredObjectType, ImportResult> counter = new StoredObjectCounter<StoredObjectType, ImportResult>(
		StoredObjectType.class, ImportResult.class);

	public Importer(ObjectStore objectStore, ContentStreamStore fileSystem) {
		super(objectStore, fileSystem);
	}

	public Importer(ObjectStore objectStore, ContentStreamStore fileSystem, int threadCount) {
		super(objectStore, fileSystem, threadCount);
	}

	public Importer(ObjectStore objectStore, ContentStreamStore fileSystem, int threadCount, int backlogSize) {
		super(objectStore, fileSystem, threadCount, backlogSize);
	}

	public Importer(ObjectStore objectStore, ContentStreamStore fileSystem, Logger output) {
		super(objectStore, fileSystem, output);
	}

	public Importer(ObjectStore objectStore, ContentStreamStore fileSystem, Logger output, int threadCount) {
		super(objectStore, fileSystem, output, threadCount);
	}

	public Importer(ObjectStore objectStore, ContentStreamStore fileSystem, Logger output, int threadCount,
		int backlogSize) {
		super(objectStore, fileSystem, output, threadCount, backlogSize);
	}

	public final StoredObjectCounter<StoredObjectType, ImportResult> getCounter() {
		return this.counter;
	}

	public void doImport(final DctmSessionManager sessionManager, boolean postProcess) throws DfException,
		CMSMFException {

		// First things first...we should only do this if the target repo ID
		// is not the same as the previous target repo - we can tell this by
		// looking at the target mappings.
		// this.log.info("Clearing all previous mappings");
		// objectStore.clearAllMappings();

		final ObjectStore objectStore = getObjectStore();
		final ContentStreamStore fileSystem = getContentStreamStore();
		final int threadCount = getThreadCount();
		final int backlogSize = getBacklogSize();
		final AtomicInteger activeCounter = new AtomicInteger(0);
		final Collection<StoredObject<?>> exitValue = new ArrayList<StoredObject<?>>(0);
		final BlockingQueue<Collection<StoredObject<?>>> workQueue = new ArrayBlockingQueue<Collection<StoredObject<?>>>(
			threadCount * backlogSize);
		final ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		this.counter.reset();
		final SynchronizedCounter batchCounter = new SynchronizedCounter(0);

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				activeCounter.incrementAndGet();
				final Logger output = getOutput();
				IDfSession session = null;
				try {
					while (!Thread.interrupted()) {
						if (Importer.this.log.isDebugEnabled()) {
							Importer.this.log.debug("Polling the queue...");
						}
						final Collection<StoredObject<?>> batch;
						// increase the waiter count
						try {
							batch = workQueue.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}

						if (batch == exitValue) {
							Importer.this.log.info("Exiting the export polling loop");
							return;
						}

						if ((batch == null) || batch.isEmpty()) {
							// Shouldn't happen, but still
							Importer.this.log.warn(String.format(
								"An invalid value made it into the work queue somehow: %s", batch));
							continue;
						}

						if (Importer.this.log.isDebugEnabled()) {
							Importer.this.log.debug(String.format("Polled a batch with %d items", batch.size()));
						}

						session = sessionManager.acquireSession();
						if (Importer.this.log.isDebugEnabled()) {
							Importer.this.log
								.debug(String.format("Got IDfSession [%s]", DfUtils.getSessionId(session)));
						}

						try {
							boolean failBatch = false;
							for (StoredObject<?> next : batch) {
								if (failBatch) {
									final ImportResult result = ImportResult.SKIPPED;
									Importer.this.log.error(String.format(
										"Batch has been failed - will not process [%s](%s) (%s)", next.getLabel(),
										next.getId(), result.name()));
									objectImportCompleted(next, result);
									Importer.this.counter.increment(next.getType(), result);
									continue;
								}

								DctmTransferContext ctx = new DefaultTransferContext(next.getId(), session, objectStore,
									fileSystem, output);
								SaveResult result = null;
								final StoredObjectType storedType = next.getType();
								final DctmObjectType type = DctmTranslator.translateType(storedType);
								try {
									objectImportStarted(next);
									result = next.saveToCMS(ctx);
									objectImportCompleted(next, result);
									if (Importer.this.log.isDebugEnabled()) {
										Importer.this.log.debug(String.format("Persisted (%s) %s", result, next));
									}
								} catch (Throwable t) {
									objectImportFailed(next, t);
									result = null;
									// Log the error, move on
									Importer.this.log.error(String.format("Exception caught processing %s", next), t);
									if (type.isFailureInterruptsBatch()) {
										// If we're supposed to kill the batch, fail all the other
										// objects
										failBatch = true;
										Importer.this.log
										.debug(String
											.format(
												"Objects of type [%s] require that the remainder of the batch fail if an object fails",
												type));
										continue;
									}
								} finally {
									Importer.this.counter.increment(next.getType(),
										(result != null ? result.getResult() : ImportResult.FAILED));
									batchCounter.increment();
								}
							}
						} finally {
							// Paranoid...yes :)
							try {
								sessionManager.releaseSession(session);
							} finally {
								session = null;
							}
						}
					}
				} finally {
					Importer.this.log.debug("Worker exiting...");
					activeCounter.decrementAndGet();
					// Just in case
					if (session != null) {
						sessionManager.releaseSession(session);
					}
				}
			}
		};

		try {
			final Map<StoredObjectType, Integer> containedTypes = objectStore.getStoredObjectTypes();
			final StoredObjectHandler<IDfValue> handler = new StoredObjectHandler<IDfValue>() {
				private String batchId = null;
				private List<StoredObject<?>> batch = null;

				@Override
				public boolean newBatch(String batchId) throws StorageException {
					this.batch = new LinkedList<StoredObject<?>>();
					this.batchId = batchId;
					return true;
				}

				@Override
				public boolean handleObject(StoredObject<IDfValue> dataObject) {
					this.batch.add(dataObject);
					return true;
				}

				@Override
				public boolean closeBatch(boolean ok) throws StorageException {
					if ((this.batch == null) || this.batch.isEmpty()) { return true; }
					StoredObject<?> sample = this.batch.get(0);
					StoredObjectType storedType = sample.getType();
					DctmObjectType type = DctmTranslator.translateType(storedType);
					if (type.isBatchingSupported() && (type.getPeerDependencyType() == DctmDependencyType.HIERARCHY)) {
						// Batch items are to be run in parallel, waiting for all of them to
						// complete before we return
						batchCounter.setValue(0);
						for (StoredObject<?> o : this.batch) {
							List<StoredObject<?>> l = new ArrayList<StoredObject<?>>(1);
							l.add(o);
							try {
								workQueue.put(l);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								String msg = String.format(
									"Thread interrupted while trying to submit the batch %s containing [%s]",
									this.batchId, this.batch);
								if (Importer.this.log.isDebugEnabled()) {
									Importer.this.log.warn(msg, e);
								} else {
									Importer.this.log.warn(msg);
								}
								// Thread is interrupted, take that as a sign to terminate
								return false;
							}
						}
						try {
							batchCounter.waitForValue(this.batch.size());
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							String msg = String.format(
								"Thread interrupted while trying to submit the batch %s containing [%s]", this.batchId,
								this.batch);
							if (Importer.this.log.isDebugEnabled()) {
								Importer.this.log.warn(msg, e);
							} else {
								Importer.this.log.warn(msg);
							}
							// Thread is interrupted, take that as a sign to terminate
							return false;
						}
					} else {
						try {
							workQueue.put(this.batch);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							String msg = String.format(
								"Thread interrupted while trying to submit the batch %s containing [%s]", this.batchId,
								this.batch);
							if (Importer.this.log.isDebugEnabled()) {
								Importer.this.log.warn(msg, e);
							} else {
								Importer.this.log.warn(msg);
							}
							return false;
						} finally {
							this.batch = null;
							this.batchId = null;
						}
					}
					// TODO: Perhaps check the error threshold
					return true;
				}

				@Override
				public boolean handleException(SQLException e) {
					return true;
				}
			};

			List<Future<?>> futures = new ArrayList<Future<?>>();
			List<Collection<StoredObject<?>>> remaining = new ArrayList<Collection<StoredObject<?>>>();
			importStarted(containedTypes);
			for (final StoredObjectType storedType : StoredObjectType.values()) {
				DctmObjectType type = DctmTranslator.translateType(storedType);
				if (type.isSurrogate()) {
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("Skipping type %s because it is a surrogate of [%s]", type.name(),
							type.getSurrogateOf()));
					}
					continue;
				}
				final Integer total = containedTypes.get(type);
				if (total == null) {
					this.log.warn(String.format("No %s objects are contained in the export", type.name()));
					continue;
				}

				if (total < 1) {
					this.log.warn(String.format("No %s objects available"));
					continue;
				}

				objectTypeImportStarted(type.getCmsType(), total);

				// Start the workers
				futures.clear();
				// If the type is hierarchical, but doesn't support batching, we must serialize
				final int workerCount = ((type.getPeerDependencyType() == DctmDependencyType.HIERARCHY)
					&& !type.isBatchingSupported() ? 1 : threadCount);
				for (int i = 0; i < workerCount; i++) {
					futures.add(executor.submit(worker));
				}

				this.log.info(String.format("%d %s objects available, starting deserialization", total, type.name()));
				objectStore.loadObjects(DctmTranslator.INSTANCE, type.getCmsType(), handler);

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
					objectTypeImportFinished(type.getCmsType(), this.counter.getCounters(type.getCmsType()));
					workQueue.drainTo(remaining);
					for (Collection<StoredObject<?>> v : remaining) {
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

			if (postProcess) {
				this.log.info("Started executing import post process jobs");
				IDfSession session = sessionManager.acquireSession();
				try {
					// Run a dm_clean job to clean up any unwanted internal acls created
					CMSMFUtils.runDctmJob(session, "dm_DMClean");

					// Run a UpdateStats job
					CMSMFUtils.runDctmJob(session, "dm_UpdateStats");
				} catch (DfException e) {
					this.log.error("Error running a post import process steps.", e);
				} finally {
					sessionManager.releaseSession(session);
				}
				this.log.info("Finished executing import post process jobs");
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
			importConcluded(this.counter.getCummulative());
			for (StoredObjectType storedType : StoredObjectType.values()) {
				DctmObjectType type = DctmTranslator.translateType(storedType);
				if (type.isSurrogate()) {
					continue;
				}
				this.log.info(String.format("Action report for %s:%n%s", type.name(),
					this.counter.generateReport(storedType)));
			}
			this.log.info(String.format("Summary Report:%n%s", this.counter.generateCummulativeReport()));
		}
	}

	private void importStarted(Map<StoredObjectType, Integer> summary) {
		for (ImportEngineListener l : getListeners()) {
			try {
				l.importStarted(summary);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectTypeImportStarted(StoredObjectType objectType, int totalObjects) {
		for (ImportEngineListener l : getListeners()) {
			try {
				l.objectTypeImportStarted(objectType, totalObjects);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectImportStarted(StoredObject<?> object) {
		for (ImportEngineListener l : getListeners()) {
			try {
				l.objectImportStarted(object);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectImportCompleted(StoredObject<?> object, ImportResult result) {
		for (ImportEngineListener l : getListeners()) {
			try {
				l.objectImportCompleted(object, result, null, null);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectImportCompleted(StoredObject<?> object, SaveResult result) {
		for (ImportEngineListener l : getListeners()) {
			try {
				l.objectImportCompleted(object, result.getResult(), result.getObjectLabel(), result.getObjectId());
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectImportFailed(StoredObject<?> object, Throwable thrown) {
		for (ImportEngineListener l : getListeners()) {
			try {
				l.objectImportFailed(object, thrown);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectTypeImportFinished(StoredObjectType objectType, Map<ImportResult, Integer> counters) {
		for (ImportEngineListener l : getListeners()) {
			try {
				l.objectTypeImportFinished(objectType, counters);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void importConcluded(Map<ImportResult, Integer> counters) {
		for (ImportEngineListener l : getListeners()) {
			try {
				l.importFinished(counters);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}
}