/**
 *
 */

package com.delta.cmsmf.engine;

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

import com.delta.cmsmf.cms.CmsCounter;
import com.delta.cmsmf.cms.CmsDependencyType;
import com.delta.cmsmf.cms.CmsFileSystem;
import com.delta.cmsmf.cms.CmsImportResult;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObject.SaveResult;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.CmsTransferContext;
import com.delta.cmsmf.cms.DefaultTransferContext;
import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.delta.cmsmf.cms.storage.CmsObjectStore.ObjectHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.CMSMFUtils;
import com.delta.cmsmf.utils.DfUtils;
import com.delta.cmsmf.utils.SynchronizedCounter;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsImporter extends CmsTransferEngine<CmsImportEventListener> {

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

	public final CmsCounter<CmsImportResult> getCounter() {
		return this.counter;
	}

	public void doImport(final CmsObjectStore objectStore, final DctmSessionManager sessionManager,
		final CmsFileSystem fileSystem, boolean postProcess) throws DfException, CMSMFException {

		// First things first...we should only do this if the target repo ID
		// is not the same as the previous target repo - we can tell this by
		// looking at the target mappings.
		// this.log.info("Clearing all previous mappings");
		// objectStore.clearAllMappings();

		final int threadCount = getThreadCount();
		final int backlogSize = getBacklogSize();
		final AtomicInteger activeCounter = new AtomicInteger(0);
		final Collection<CmsObject<?>> exitValue = new ArrayList<CmsObject<?>>(0);
		final BlockingQueue<Collection<CmsObject<?>>> workQueue = new ArrayBlockingQueue<Collection<CmsObject<?>>>(
			threadCount * backlogSize);
		final ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		this.counter.reset();
		final SynchronizedCounter batchCounter = new SynchronizedCounter(0);

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				final IDfSession session = sessionManager.acquireSession();
				if (CmsImporter.this.log.isDebugEnabled()) {
					CmsImporter.this.log.debug(String.format("Got IDfSession [%s]", DfUtils.getSessionId(session)));
				}

				activeCounter.incrementAndGet();
				try {
					while (!Thread.interrupted()) {
						if (CmsImporter.this.log.isDebugEnabled()) {
							CmsImporter.this.log.debug("Polling the queue...");
						}
						final Collection<CmsObject<?>> batch;
						// increase the waiter count
						try {
							batch = workQueue.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}

						if (batch == exitValue) {
							CmsImporter.this.log.info("Exiting the export polling loop");
							return;
						}

						if ((batch == null) || batch.isEmpty()) {
							// Shouldn't happen, but still
							CmsImporter.this.log.warn(String.format(
								"An invalid value made it into the work queue somehow: %s", batch));
							continue;
						}

						if (CmsImporter.this.log.isDebugEnabled()) {
							CmsImporter.this.log.debug(String.format("Polled a batch with %d items", batch.size()));
						}

						for (CmsObject<?> next : batch) {
							CmsTransferContext ctx = new DefaultTransferContext(next.getId(), session, objectStore,
								fileSystem);
							SaveResult result = null;
							try {
								objectImportStarted(next);
								result = next.saveToCMS(ctx);
								objectImportCompleted(next, result.getResult());
								if (CmsImporter.this.log.isDebugEnabled()) {
									CmsImporter.this.log.debug(String.format("Persisted (%s) %s", result, next));
								}
							} catch (Throwable t) {
								objectImportFailed(next, t);
								result = null;
								// Log the error, move on
								CmsImporter.this.log.error(String.format("Exception caught processing %s", next), t);
							} finally {
								CmsImporter.this.counter.increment(next, (result != null ? result.getResult()
									: CmsImportResult.FAILED));
								batchCounter.increment();
							}
						}
					}
				} finally {
					CmsImporter.this.log.debug("Worker exiting...");
					activeCounter.decrementAndGet();
					sessionManager.releaseSession(session);
				}
			}
		};

		try {
			final Map<CmsObjectType, Integer> containedTypes = objectStore.getStoredObjectTypes();
			final ObjectHandler handler = new ObjectHandler() {
				private String batchId = null;
				private List<CmsObject<?>> batch = null;

				@Override
				public boolean newBatch(String batchId) throws CMSMFException {
					this.batch = new LinkedList<CmsObject<?>>();
					this.batchId = batchId;
					return true;
				}

				@Override
				public void handle(CmsObject<?> dataObject) {
					this.batch.add(dataObject);
				}

				@Override
				public boolean closeBatch(boolean ok) throws CMSMFException {
					if ((this.batch == null) || this.batch.isEmpty()) { return true; }
					CmsObject<?> sample = this.batch.get(0);
					CmsObjectType type = sample.getType();
					if (type.isBatchingSupported() && (type.getPeerDependencyType() == CmsDependencyType.HIERARCHY)) {
						// Batch items are to be run in parallel, waiting for all of them to
						// complete before we return
						batchCounter.setValue(0);
						for (CmsObject<?> o : this.batch) {
							List<CmsObject<?>> l = new ArrayList<CmsObject<?>>(1);
							l.add(o);
							try {
								workQueue.put(l);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								String msg = String.format(
									"Thread interrupted while trying to submit the batch %s containing [%s]",
									this.batchId, this.batch);
								if (CmsImporter.this.log.isDebugEnabled()) {
									CmsImporter.this.log.warn(msg, e);
								} else {
									CmsImporter.this.log.warn(msg);
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
							if (CmsImporter.this.log.isDebugEnabled()) {
								CmsImporter.this.log.warn(msg, e);
							} else {
								CmsImporter.this.log.warn(msg);
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
							if (CmsImporter.this.log.isDebugEnabled()) {
								CmsImporter.this.log.warn(msg, e);
							} else {
								CmsImporter.this.log.warn(msg);
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
			};

			List<Future<?>> futures = new ArrayList<Future<?>>();
			List<Collection<CmsObject<?>>> remaining = new ArrayList<Collection<CmsObject<?>>>();
			importStarted(containedTypes);
			for (CmsObjectType type : CmsObjectType.values()) {
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

				objectTypeImportStarted(type, total);

				// Start the workers
				futures.clear();
				// If the type is hierarchical, but doesn't support batching, we must serialize
				final int workerCount = ((type.getPeerDependencyType() == CmsDependencyType.HIERARCHY)
					&& !type.isBatchingSupported() ? 1 : threadCount);
				for (int i = 0; i < workerCount; i++) {
					futures.add(executor.submit(worker));
				}

				this.log.info(String.format("%d %s objects available, starting deserialization", total, type.name()));
				objectStore.deserializeObjects(type.getCmsObjectClass(), handler);

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
					objectTypeImportFinished(type, this.counter.getCounters(type));
					workQueue.drainTo(remaining);
					for (Collection<CmsObject<?>> v : remaining) {
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
			for (CmsObjectType type : CmsObjectType.values()) {
				if (type.isSurrogate()) {
					continue;
				}
				this.log
					.info(String.format("Action report for %s:%n%s", type.name(), this.counter.generateReport(type)));
			}
			this.log.info(String.format("Summary Report:%n%s", this.counter.generateCummulativeReport()));
		}
	}

	private void importStarted(Map<CmsObjectType, Integer> summary) {
		for (CmsImportEventListener l : getListeners()) {
			try {
				l.importStarted(summary);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectTypeImportStarted(CmsObjectType objectType, int totalObjects) {
		for (CmsImportEventListener l : getListeners()) {
			try {
				l.objectTypeImportStarted(objectType, totalObjects);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectImportStarted(CmsObject<?> object) {
		for (CmsImportEventListener l : getListeners()) {
			try {
				l.objectImportStarted(object);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectImportCompleted(CmsObject<?> object, CmsImportResult cmsImportResult) {
		for (CmsImportEventListener l : getListeners()) {
			try {
				l.objectImportCompleted(object, cmsImportResult);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectImportFailed(CmsObject<?> object, Throwable thrown) {
		for (CmsImportEventListener l : getListeners()) {
			try {
				l.objectImportFailed(object, thrown);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void objectTypeImportFinished(CmsObjectType objectType, Map<CmsImportResult, Integer> counters) {
		for (CmsImportEventListener l : getListeners()) {
			try {
				l.objectTypeImportCompleted(objectType, counters);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}

	private void importConcluded(Map<CmsImportResult, Integer> counters) {
		for (CmsImportEventListener l : getListeners()) {
			try {
				l.importConcluded(counters);
			} catch (Throwable t) {
				this.log.warn("Exception caught in event propagation", t);
			}
		}
	}
}