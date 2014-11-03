/**
 *
 */

package com.armedia.cmf.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueEncoderException;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;

/**
 * @author diego
 *
 */
public final class ExportEngine<S, W extends SessionWrapper<S>, T, V> extends TransferEngine<ExportEngineListener>
implements ExportEngineListener {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private Long exportObject(final ObjectStore<?, ?> objectStore, final ContentStreamStore streamStore,
		Exporter<S, T, V> exporter, S session, StoredObject<V> marshaled, T sourceObject, ExportContext<S, T, V> ctx)
		throws ExportException, StorageException, StoredValueEncoderException, UnsupportedObjectTypeException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to operate with"); }
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to export"); }
		if (sourceObject == null) { throw new IllegalArgumentException("Must provide the original object to export"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a context to operate in"); }

		final String id = marshaled.getId();
		final StoredObjectType type = marshaled.getType();
		final String label = String.format("%s [%s](%s)", type, marshaled.getLabel(), id);

		// First, make sure other threads don't work on this same object
		if (!objectStore.lockForStorage(type, id)) {
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("%s is already locked for storage", label));
			}
			return null;
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Locked %s for storage", label));
		}

		// Make sure the object hasn't already been exported
		if (objectStore.isStored(type, id)) {
			// Should be impossible, but still guard against it
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("%s was locked for storage by this thread, but is already stored...",
					label));
			}
			return null;
		}

		Collection<T> referenced;
		try {
			referenced = exporter.identifyRequirements(session, sourceObject);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to identify the requirements for %s", label), e);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("%s requires %d objects for successful storage", label, referenced.size()));
		}
		for (T requirement : referenced) {
			StoredObject<V> req = exporter.marshal(session, requirement);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("%s requires %s [%s](%s)", label, req.getType(), req.getLabel(),
					req.getId()));
			}
			exportObject(objectStore, streamStore, exporter, session, req, requirement, ctx);
		}

		final Long ret = objectStore.storeObject(marshaled, exporter.getTranslator());
		if (ret == null) {
			// Should be impossible, but still guard against it
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("%s was stored by another thread", label));
			}
			return null;
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Successfully stored %s as object # %d", label, ret));
		}

		try {
			referenced = exporter.identifyDependents(session, sourceObject);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to identify the dependents for %s", label), e);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("%s has %d dependent objects to store", label, referenced.size()));
		}
		for (T dependent : referenced) {
			StoredObject<V> dep = exporter.marshal(session, dependent);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("%s requires %s [%s](%s)", label, dep.getType(), dep.getLabel(),
					dep.getId()));
			}
			exportObject(objectStore, streamStore, exporter, session, dep, dependent, ctx);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Executing supplemental storage for %s", label));
		}
		try {
			exporter.storeContent(session, sourceObject, streamStore);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to execute the supplemental storage for %s", label), e);
		}

		return ret;
	}

	public final void runExport(final Logger output, final ObjectStore<?, ?> objectStore,
		final ContentStreamStore streamStore, final Exporter<S, T, V> exporter, Map<String, Object> settings)
			throws ExportException, StorageException {
		// We get this at the very top because if this fails, there's no point in continuing.
		final SessionFactory<S> sessionFactory = exporter.getSessionFactory();
		final SessionWrapper<S> baseSession;
		try {
			baseSession = sessionFactory.acquireSession();
		} catch (Exception e) {
			throw new ExportException("Failed to obtain the main export session", e);
		}

		final int threadCount;
		final int backlogSize;
		// Ensure nobody changes this under our feet
		synchronized (this) {
			threadCount = getThreadCount();
			backlogSize = getBacklogSize();
		}

		final AtomicInteger activeCounter = new AtomicInteger(0);
		final ExportTarget exitValue = new ExportTarget();
		final BlockingQueue<ExportTarget> workQueue = new ArrayBlockingQueue<ExportTarget>(backlogSize);
		final ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				final SessionWrapper<S> session;
				try {
					session = sessionFactory.acquireSession();
				} catch (Exception e) {
					ExportEngine.this.log.error("Failed to obtain a worker session", e);
					return;
				}
				if (ExportEngine.this.log.isDebugEnabled()) {
					ExportEngine.this.log.debug(String.format("Got session [%s]", session.getId()));
				}
				final S s = session.getWrapped();
				activeCounter.incrementAndGet();
				try {
					while (!Thread.interrupted()) {
						if (ExportEngine.this.log.isDebugEnabled()) {
							ExportEngine.this.log.debug("Polling the queue...");
						}
						final ExportTarget next;
						try {
							next = workQueue.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}

						// We compare instances, and not values, because we're interested
						// in seeing if the EXACT exit value flag is used, not one that looks
						// the same out of some unfortunate coincidence. By checking instances,
						// we ensure that we will not exit the loop prematurely due to a value
						// collision.
						if (next == exitValue) {
							// Work complete
							ExportEngine.this.log.info("Exiting the export polling loop");
							return;
						}

						if (ExportEngine.this.log.isDebugEnabled()) {
							ExportEngine.this.log.debug(String.format("Polled %s", next));
						}

						boolean tx = false;
						boolean ok = false;
						try {
							// Begin transaction
							tx = session.begin();
							final T sourceObject = exporter.getObject(s, next.getType(), next.getId());
							if (sourceObject == null) {
								// No object found with that ID...
								ExportEngine.this.log.warn(String.format("No %s object found with ID[%s]",
									next.getType(), next.getId()));
								continue;
							}

							if (ExportEngine.this.log.isDebugEnabled()) {
								ExportEngine.this.log.debug(String.format("Exporting the source %s object with ID[%s]",
									next.getType(), next.getId()));
							}
							ExportContext<S, T, V> ctx = new ExportContext<S, T, V>(next.getId(), session.getWrapped(),
								output);
							objectExportStarted(next.getType(), next.getId());
							StoredObject<V> marshaled = exporter.marshal(s, sourceObject);
							Long result = exportObject(objectStore, streamStore, exporter, s, marshaled, sourceObject,
								ctx);
							if (marshaled != null) {
								if (ExportEngine.this.log.isDebugEnabled()) {
									ExportEngine.this.log.debug(String.format("Exported %s [%s](%s) in position %d",
										marshaled.getType(), marshaled.getLabel(), marshaled.getId(), result));
								}
								objectExportCompleted(marshaled, result);
							} else {
								if (ExportEngine.this.log.isDebugEnabled()) {
									ExportEngine.this.log.debug(String.format(
										"%s object with ID[%s] was already exported", next.getType(), next.getId()));
								}
								objectSkipped(next.getType(), next.getId());
							}
							ok = true;
						} catch (Exception e) {
							ExportEngine.this.log.error(
								String.format("Failed to export %s object with ID[%s]", next.getType(), next.getId()),
								e);
							objectExportFailed(next.getType(), next.getId(), e);
							if (tx) {
								if (ok) {
									session.commit();
								} else {
									session.rollback();
								}
							}
						}
					}
				} finally {
					activeCounter.decrementAndGet();
					session.close();
				}
			}
		};

		// Fire off the workers
		List<Future<?>> futures = new ArrayList<Future<?>>(threadCount);
		for (int i = 0; i < threadCount; i++) {
			futures.add(executor.submit(worker));
		}
		executor.shutdown();

		final Iterable<ExportTarget> results;
		try {
			results = exporter.findExportResults(baseSession.getWrapped(), settings);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to obtain the export results with settings: %s", settings),
				e);
		}

		try {
			int counter = 0;
			// 1: run the query for the given predicate
			exportStarted(settings);
			// 2: iterate over the results, gathering up the object IDs
			for (final ExportTarget target : results) {
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("Processing item %s", target));
				}
				try {
					workQueue.put(target);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					if (this.log.isDebugEnabled()) {
						this.log.warn(String.format("Thread interrupted after reading %d object targets", counter), e);
					} else {
						this.log.warn(String.format("Thread interrupted after reading %d objects targets", counter));
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
				List<ExportTarget> remaining = new ArrayList<ExportTarget>();
				workQueue.drainTo(remaining);
				for (ExportTarget v : remaining) {
					if (v == exitValue) {
						continue;
					}
					this.log.error(String.format("WORK LEFT PENDING IN THE QUEUE: %s", v));
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
			baseSession.close(false);

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
		}
	}

	@Override
	public void exportStarted(Map<String, Object> exportSettings) {
		for (ExportEngineListener l : getListeners()) {
			try {
				l.exportStarted(exportSettings);
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught during listener propagation", e);
				}
			}
		}
	}

	@Override
	public void objectExportStarted(StoredObjectType objectType, String objectId) {
		for (ExportEngineListener l : getListeners()) {
			try {
				l.objectExportStarted(objectType, objectId);
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught during listener propagation", e);
				}
			}
		}
	}

	@Override
	public void objectExportCompleted(StoredObject<?> object, Long objectNumber) {
		for (ExportEngineListener l : getListeners()) {
			try {
				l.objectExportCompleted(object, objectNumber);
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught during listener propagation", e);
				}
			}
		}
	}

	@Override
	public void objectSkipped(StoredObjectType objectType, String objectId) {
		for (ExportEngineListener l : getListeners()) {
			try {
				l.objectSkipped(objectType, objectId);
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught during listener propagation", e);
				}
			}
		}
	}

	@Override
	public void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown) {
		for (ExportEngineListener l : getListeners()) {
			try {
				l.objectExportFailed(objectType, objectId, thrown);
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught during listener propagation", e);
				}
			}
		}
	}

	@Override
	public void exportFinished(Map<StoredObjectType, Integer> summary) {
		for (ExportEngineListener l : getListeners()) {
			try {
				l.exportFinished(summary);
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught during listener propagation", e);
				}
			}
		}
	}
}