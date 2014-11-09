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
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
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
TransferEngine<S, T, V, ExportEngineListener> {

	private static final String REFERRENT_ID = "${CONTENT_PATH}$";
	private static final String REFERRENT_TYPE = "${REFERRENT_TYPE}$";
	private static final String CONTENT_PATH = "${REFERRENT_ID}$";

	private class Result {
		private final Long objectNumber;
		private final StoredObject<V> marshaled;

		public Result(Long objectNumber, StoredObject<V> marshaled) {
			this.objectNumber = objectNumber;
			this.marshaled = marshaled;
		}
	}

	private Logger log = LoggerFactory.getLogger(getClass());

	private class ListenerDelegator implements ExportEngineListener {

		private final Collection<ExportEngineListener> listeners = getListeners();

		@Override
		public void exportStarted(Map<String, ?> exportSettings) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.exportStarted(exportSettings);
				} catch (Exception e) {
					if (ExportEngine.this.log.isDebugEnabled()) {
						ExportEngine.this.log.error("Exception caught during listener propagation", e);
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
					if (ExportEngine.this.log.isDebugEnabled()) {
						ExportEngine.this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportCompleted(StoredObject<?> object, Long objectNumber) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportCompleted(object, objectNumber);
				} catch (Exception e) {
					if (ExportEngine.this.log.isDebugEnabled()) {
						ExportEngine.this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectSkipped(StoredObjectType objectType, String objectId) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectSkipped(objectType, objectId);
				} catch (Exception e) {
					if (ExportEngine.this.log.isDebugEnabled()) {
						ExportEngine.this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportFailed(objectType, objectId, thrown);
				} catch (Exception e) {
					if (ExportEngine.this.log.isDebugEnabled()) {
						ExportEngine.this.log.error("Exception caught during listener propagation", e);
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
					if (ExportEngine.this.log.isDebugEnabled()) {
						ExportEngine.this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}
	}

	protected abstract String getObjectId(T sourceObject);

	protected abstract String calculateLabel(T sourceObject) throws Exception;

	private Result exportObject(final ObjectStore<?, ?> objectStore, final ContentStreamStore streamStore, S session,
		final ExportTarget referrent, final ExportTarget target, T sourceObject, C ctx,
		ListenerDelegator listenerDelegator) throws ExportException, StorageException, StoredValueEncoderException,
		UnsupportedObjectTypeException {
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
		final String contentPath;
		try {
			contentPath = storeContent(session, marshaled, sourceObject, streamStore);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to execute the supplemental storage for %s", label), e);
		}

		if (contentPath != null) {
			StoredProperty<V> p = new StoredProperty<>(ExportEngine.CONTENT_PATH, StoredDataType.STRING, true);
			p.setValue(getValue(StoredDataType.STRING, contentPath));
			marshaled.setProperty(p);
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

	public final void runExport(final Logger output, final ObjectStore<?, ?> objectStore,
		final ContentStreamStore streamStore, Map<String, ?> settings) throws ExportException, StorageException {
		// We get this at the very top because if this fails, there's no point in continuing.

		final SessionFactory<S> sessionFactory = newSessionFactory();
		try {
			sessionFactory.init(new CfgTools(settings));
		} catch (Exception e) {
			throw new ExportException("Failed to configure the session factory to carry out the export", e);
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
		final ExportTarget exitValue = new ExportTarget();
		final BlockingQueue<ExportTarget> workQueue = new ArrayBlockingQueue<ExportTarget>(backlogSize);
		final ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());
		final ListenerDelegator listenerDelegator = new ListenerDelegator();

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
							final T sourceObject = getObject(s, next.getType(), next.getId());
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

							C ctx = newContext(next.getId(), session.getWrapped(), output);
							initContext(ctx);
							listenerDelegator.objectExportStarted(next.getType(), next.getId());
							Result result = exportObject(objectStore, streamStore, s, null, next, sourceObject, ctx,
								listenerDelegator);
							if (result != null) {
								if (ExportEngine.this.log.isDebugEnabled()) {
									ExportEngine.this.log.debug(String.format("Exported %s [%s](%s) in position %d",
										result.marshaled.getType(), result.marshaled.getLabel(),
										result.marshaled.getId(), result.objectNumber));
								}
								listenerDelegator.objectExportCompleted(result.marshaled, result.objectNumber);
							} else {
								if (ExportEngine.this.log.isDebugEnabled()) {
									ExportEngine.this.log.debug(String.format(
										"%s object with ID[%s] was already exported", next.getType(), next.getId()));
								}
								listenerDelegator.objectSkipped(next.getType(), next.getId());
							}
							ok = true;
						} catch (Throwable t) {
							ExportEngine.this.log.error(
								String.format("Failed to export %s object with ID[%s]", next.getType(), next.getId()),
								t);
							listenerDelegator.objectExportFailed(next.getType(), next.getId(), t);
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

		final Iterator<ExportTarget> results;
		try {
			results = findExportResults(baseSession.getWrapped(), settings);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to obtain the export results with settings: %s", settings),
				e);
		}

		try {
			int counter = 0;
			// 1: run the query for the given predicate
			listenerDelegator.exportStarted(settings);
			// 2: iterate over the results, gathering up the object IDs
			while (results.hasNext()) {
				final ExportTarget target = results.next();
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
			this.log.info(String.format("Submitted the entire export workload (%d objects)", counter));

			// Ask the workers to exit civilly
			this.log.info("Signaling work completion for the workers");
			boolean waitCleanly = true;
			for (int i = 0; i < activeCounter.get(); i++) {
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
							this.log.warn(
								"Interrupted while waiting for an executor thread to exit, forcing the shutdown", e);
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
	}

	protected abstract C newContext(String rootId, S session, Logger output);

	protected void initContext(C ctx) {
	}

	protected abstract Iterator<ExportTarget> findExportResults(S session, Map<String, ?> settings) throws Exception;

	protected abstract T getObject(S session, StoredObjectType type, String id) throws Exception;

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
			referrentType.addValue(getValue(StoredDataType.STRING, referrent.getType().name()));
			marshaled.setProperty(referrentType);
			StoredProperty<V> referrentId = new StoredProperty<V>(ExportEngine.REFERRENT_ID, StoredDataType.STRING,
				false);
			referrentId.addValue(getValue(StoredDataType.STRING, referrent.getId()));
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
		return new ExportTarget(StoredObjectType.valueOf(type), id);
	}

	protected final String getContentPath(StoredObject<V> marshaled) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		StoredProperty<V> contentPath = marshaled.getProperty(ExportEngine.CONTENT_PATH);
		if (contentPath == null) { return null; }
		return Tools.toString(contentPath.getValue(), true);
	}

	protected abstract V getValue(StoredDataType type, Object value);

	protected abstract StoredObject<V> marshal(S session, T object) throws ExportException;

	protected abstract String storeContent(S session, StoredObject<V> marshalled, T object,
		ContentStreamStore streamStore) throws Exception;

	public static ExportEngine<?, ?, ?, ?, ?> getExportEngine(String targetName) {
		return TransferEngine.getTransferEngine(ExportEngine.class, targetName);
	}
}