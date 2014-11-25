/**
 *
 */

package com.armedia.cmf.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectCounter;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValueEncoderException;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class ExportEngine<S, W extends SessionWrapper<S>, T, V, C extends ExportContext<S, T, V>> extends
TransferEngine<S, T, V, C, ExportEngineListener> {

	private static final String REFERRENT_ID = "${REFERRENT_ID}$";
	private static final String REFERRENT_TYPE = "${REFERRENT_TYPE}$";

	private class Result {
		private final Long objectNumber;
		private final StoredObject<V> marshaled;

		public Result(Long objectNumber, StoredObject<V> marshaled) {
			this.objectNumber = objectNumber;
			this.marshaled = marshaled;
		}
	}

	private class ExportListenerDelegator extends ListenerDelegator<ExportResult> implements ExportEngineListener {

		private final Collection<ExportEngineListener> listeners = getListeners();

		private ExportListenerDelegator(StoredObjectCounter<ExportResult> counter) {
			super(counter);
		}

		@Override
		public void exportStarted(Map<String, ?> exportSettings) {
			getStoredObjectCounter().reset();
			for (ExportEngineListener l : this.listeners) {
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
			for (ExportEngineListener l : this.listeners) {
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
			getStoredObjectCounter().increment(object.getType(), ExportResult.EXPORTED);
			for (ExportEngineListener l : this.listeners) {
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
			getStoredObjectCounter().increment(objectType, ExportResult.SKIPPED);
			for (ExportEngineListener l : this.listeners) {
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
			getStoredObjectCounter().increment(objectType, ExportResult.FAILED);
			for (ExportEngineListener l : this.listeners) {
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
			for (ExportEngineListener l : this.listeners) {
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

	protected abstract String getObjectId(T sourceObject);

	protected abstract String calculateLabel(T sourceObject) throws Exception;

	private Result exportObject(final ObjectStore<?, ?> objectStore, final ContentStore streamStore, S session,
		final ExportTarget referrent, final ExportTarget target, T sourceObject, C ctx,
		ExportListenerDelegator listenerDelegator) throws ExportException, StorageException,
		StoredValueEncoderException, UnsupportedObjectTypeException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to operate with"); }
		if (target == null) { throw new IllegalArgumentException("Must provide the original export target"); }
		if (sourceObject == null) { throw new IllegalArgumentException("Must provide the original object to export"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a context to operate in"); }
		final StoredObjectType type = target.getType();
		final String id = target.getId();
		String objectLabel = null;
		try {
			objectLabel = calculateLabel(sourceObject);
		} catch (Exception e) {
			this.log.warn(String.format("Failed to calculate object label for %s (%s)", type, id), e);
			objectLabel = String.format("{Failed to calculate object label: %s}", e.getMessage());
		}
		final String label = String.format("%s [%s](%s)", type, objectLabel, id);

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Attempting export of %s", label));
		}

		// First, make sure other threads don't work on this same object
		boolean locked = false;
		try {
			locked = objectStore.lockForStorage(type, id);
		} catch (StorageException e) {
			if (this.log.isTraceEnabled()) {
				this.log.warn(String.format("Exception caught attempting to lock an object for storage: %s", label), e);
			}
			try {
				locked = objectStore.lockForStorage(type, id);
			} catch (StorageException e2) {
				// There may be some circumstances where this is necessary
				throw new ExportException(String.format("Failed to obtain or check the lock on %s", label), e2);
			}
		}

		if (!locked) {
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("%s is already locked for storage, skipping it", label));
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

		final StoredObject<V> marshaled = marshal(session, referrent, sourceObject);
		Collection<T> referenced;
		try {
			referenced = identifyRequirements(session, marshaled, sourceObject, ctx);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to identify the requirements for %s", label), e);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("%s requires %d objects for successful storage", label, referenced.size()));
		}
		for (T requirement : referenced) {
			exportObject(objectStore, streamStore, session, target, getExportTarget(requirement), requirement, ctx,
				listenerDelegator);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Executing supplemental storage for %s", label));
		}
		final Handle contentHandle;
		try {
			contentHandle = storeContent(session, marshaled, sourceObject, streamStore);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to execute the content storage for %s", label), e);
		}

		if (contentHandle != null) {
			setContentQualifier(marshaled, contentHandle.getQualifier());
		}

		final Long ret = objectStore.storeObject(marshaled, getTranslator());
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
			referenced = identifyDependents(session, marshaled, sourceObject, ctx);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to identify the dependents for %s", label), e);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("%s has %d dependent objects to store", label, referenced.size()));
		}
		for (T dependent : referenced) {
			exportObject(objectStore, streamStore, session, target, getExportTarget(dependent), dependent, ctx,
				listenerDelegator);
		}

		return new Result(ret, marshaled);
	}

	public final StoredObjectCounter<ExportResult> runExport(final Logger output, final ObjectStore<?, ?> objectStore,
		final ContentStore contentStore, Map<String, ?> settings) throws ExportException, StorageException {
		return runExport(output, objectStore, contentStore, settings, null);
	}

	public final StoredObjectCounter<ExportResult> runExport(final Logger output, final ObjectStore<?, ?> objectStore,
		final ContentStore contentStore, Map<String, ?> settings, StoredObjectCounter<ExportResult> counter)
			throws ExportException, StorageException {
		// We get this at the very top because if this fails, there's no point in continuing.

		final CfgTools configuration = new CfgTools(settings);
		final SessionFactory<S> sessionFactory;
		try {
			sessionFactory = newSessionFactory(configuration);
		} catch (Exception e) {
			throw new ExportException("Failed to configure the session factory to carry out the export", e);
		}

		final ContextFactory<S, T, V, C, ?> contextFactory;
		try {
			try {
				contextFactory = newContextFactory(configuration);
			} catch (Exception e) {
				throw new ExportException("Failed to configure the context factory to carry out the export", e);
			}

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
			final String exitValue = new String();
			final BlockingQueue<String> workQueue = new ArrayBlockingQueue<String>(backlogSize);
			final ExecutorService executor = newExecutor(threadCount);
			if (counter == null) {
				counter = new StoredObjectCounter<ExportResult>(ExportResult.class);
			}
			final ExportListenerDelegator listenerDelegator = new ExportListenerDelegator(counter);

			Runnable worker = new Runnable() {
				private final Logger log = ExportEngine.this.log;

				@Override
				public void run() {
					final SessionWrapper<S> session;
					try {
						session = sessionFactory.acquireSession();
					} catch (Exception e) {
						this.log.error("Failed to obtain a worker session", e);
						return;
					}
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("Got session [%s]", session.getId()));
					}
					final S s = session.getWrapped();
					activeCounter.incrementAndGet();
					try {
						while (!Thread.interrupted()) {
							if (this.log.isDebugEnabled()) {
								this.log.debug("Polling the queue...");
							}
							final String nextId;
							try {
								nextId = workQueue.take();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}

							// We compare instances, and not values, because we're interested
							// in seeing if the EXACT exit value flag is used, not one that looks
							// the same out of some unfortunate coincidence. By checking instances,
							// we ensure that we will not exit the loop prematurely due to a value
							// collision.
							if (nextId == exitValue) {
								// Work complete
								this.log.info("Exiting the export polling loop");
								return;
							}

							if (this.log.isDebugEnabled()) {
								this.log.debug(String.format("Polled %s", nextId));
							}

							boolean tx = false;
							boolean ok = false;
							StoredObjectType nextType = null;
							try {
								// Begin transaction
								tx = session.begin();
								final T sourceObject = getObject(s, nextId);
								if (sourceObject == null) {
									// No object found with that ID...
									this.log.warn(String.format("No object found with ID[%s]", nextId));
									continue;
								}

								final ExportTarget next = getExportTarget(sourceObject);
								nextType = next.getType();

								if (this.log.isDebugEnabled()) {
									this.log.debug(String.format("Exporting the source %s object with ID[%s]",
										next.getType(), next.getId()));
								}

								final C ctx = contextFactory.newContext(next.getId(), next.getType(),
									session.getWrapped(), output, objectStore, contentStore);
								try {
									initContext(ctx);
									listenerDelegator.objectExportStarted(next.getType(), next.getId());
									Result result = exportObject(objectStore, contentStore, s, null, next,
										sourceObject, ctx, listenerDelegator);
									if (result != null) {
										if (this.log.isDebugEnabled()) {
											this.log.debug(String.format("Exported %s [%s](%s) in position %d",
												result.marshaled.getType(), result.marshaled.getLabel(),
												result.marshaled.getId(), result.objectNumber));
										}
										listenerDelegator.objectExportCompleted(result.marshaled, result.objectNumber);
									} else {
										if (this.log.isDebugEnabled()) {
											this.log.debug(String.format("%s object with ID[%s] was already exported",
												next.getType(), next.getId()));
										}
										listenerDelegator.objectSkipped(next.getType(), next.getId());
									}
									ok = true;
								} finally {
									ctx.close();
								}
							} catch (Throwable t) {
								this.log.error(String.format("Failed to export object with ID[%s]", nextId), t);
								listenerDelegator.objectExportFailed(nextType, nextId, t);
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

			final Iterator<String> results;
			try {
				results = findExportResults(baseSession.getWrapped(), settings);
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to obtain the export results with settings: %s",
					settings), e);
			}

			try {
				int c = 0;
				// 1: run the query for the given predicate
				listenerDelegator.exportStarted(settings);
				// 2: iterate over the results, gathering up the object IDs
				while (results.hasNext()) {
					final String target = results.next();
					if (this.log.isTraceEnabled()) {
						this.log.trace(String.format("Processing item %s", target));
					}
					try {
						workQueue.put(target);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						if (this.log.isDebugEnabled()) {
							this.log.warn(String.format("Thread interrupted after reading %d object targets", c), e);
						} else {
							this.log.warn(String.format("Thread interrupted after reading %d objects targets", c));
						}
						break;
					}
				}
				this.log.info(String.format("Submitted the entire export workload (%d objects)", c));

				// Ask the workers to exit civilly
				this.log.info("Signaling work completion for the workers");
				boolean waitCleanly = true;
				for (int i = 0; i < threadCount; i++) {
					try {
						workQueue.put(exitValue);
					} catch (InterruptedException e) {
						waitCleanly = false;
						// Here we have a problem: we're timing out while adding the exit
						// values...
						this.log.warn("Interrupted while attempting to request executor thread termination", e);
						Thread.currentThread().interrupt();
						executor.shutdownNow();
						break;
					}
				}

				try {
					// We're done, we must wait until all workers are waiting
					if (waitCleanly) {
						this.log.info(String.format("Waiting for %d workers to finish processing", threadCount));
						for (Future<?> future : futures) {
							try {
								future.get();
							} catch (InterruptedException e) {
								this.log
								.warn(
									"Interrupted while waiting for an executor thread to exit, forcing the shutdown",
									e);
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
					}
				} finally {
					List<String> remaining = new ArrayList<String>();
					workQueue.drainTo(remaining);
					for (String v : remaining) {
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
				return listenerDelegator.getStoredObjectCounter();
			} finally {
				baseSession.close(false);

				Map<StoredObjectType, Integer> summary = Collections.emptyMap();
				try {
					summary = objectStore.getStoredObjectTypes();
				} catch (StorageException e) {
					this.log.warn("Exception caught attempting to get the work summary", e);
				}
				listenerDelegator.exportFinished(summary);

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
		} finally {
			sessionFactory.close();
		}
	}

	protected void initContext(C ctx) {
	}

	protected abstract Iterator<String> findExportResults(S session, Map<String, ?> settings) throws Exception;

	protected abstract T getObject(S session, String id) throws Exception;

	protected abstract Collection<T> identifyRequirements(S session, StoredObject<V> marshalled, T object, C ctx)
		throws Exception;

	protected abstract Collection<T> identifyDependents(S session, StoredObject<V> marshalled, T object, C ctx)
		throws Exception;

	protected abstract ExportTarget getExportTarget(T object) throws ExportException;

	private final StoredObject<V> marshal(S session, ExportTarget referrent, T object) throws ExportException {
		StoredObject<V> marshaled = marshal(session, object);
		// Now, add the properties to reference the referrent object
		if (referrent != null) {
			StoredProperty<V> referrentType = new StoredProperty<V>(ExportEngine.REFERRENT_TYPE, StoredDataType.STRING,
				false);
			referrentType.setValue(getValue(StoredDataType.STRING, referrent.getType().name()));
			marshaled.setProperty(referrentType);
			StoredProperty<V> referrentId = new StoredProperty<V>(ExportEngine.REFERRENT_ID, StoredDataType.STRING,
				false);
			referrentId.setValue(getValue(StoredDataType.STRING, referrent.getId()));
			marshaled.setProperty(referrentId);
		}
		return marshaled;
	}

	protected final ExportTarget getReferrent(StoredObject<V> marshaled) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		StoredProperty<V> referrentType = marshaled.getProperty(ExportEngine.REFERRENT_TYPE);
		StoredProperty<V> referrentId = marshaled.getProperty(ExportEngine.REFERRENT_ID);
		if ((referrentType == null) || (referrentId == null)) { return null; }
		String type = Tools.toString(referrentType.getValue(), true);
		String id = Tools.toString(referrentId.getValue(), true);
		if ((type == null) || (id == null)) { return null; }
		return new ExportTarget(StoredObjectType.decodeString(type), id);
	}

	protected abstract StoredObject<V> marshal(S session, T object) throws ExportException;

	protected abstract Handle storeContent(S session, StoredObject<V> marshalled, T object, ContentStore streamStore)
		throws Exception;

	public static ExportEngine<?, ?, ?, ?, ?> getExportEngine(String targetName) {
		return TransferEngine.getTransferEngine(ExportEngine.class, targetName);
	}
}