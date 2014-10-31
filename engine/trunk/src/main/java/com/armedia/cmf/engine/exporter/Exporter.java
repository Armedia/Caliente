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
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.ObjectStoreOperation;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueEncoderException;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class Exporter<T, V, S, W extends SessionWrapper<S>> implements ExportEngineListener {

	protected static final class Target {
		private final StoredObjectType type;
		private final String id;
		private final Long number;

		private Target() {
			this.type = null;
			this.id = null;
			this.number = null;
		}

		public Target(StoredObjectType type, String id) {
			this(type, id, null);
		}

		public Target(StoredObjectType type, String id, Long number) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
			if (id == null) { throw new IllegalArgumentException("Must provide an object id"); }
			this.type = type;
			this.id = id;
			this.number = number;
		}

		public StoredObjectType getType() {
			return this.type;
		}

		public String getId() {
			return this.id;
		}

		public Long getNumber() {
			return this.number;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.type, this.id);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			Target other = Target.class.cast(obj);
			if (!Tools.equals(this.type, other.type)) { return false; }
			if (!Tools.equals(this.id, other.id)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("Target [type=%s, id=%s, number=%s]", this.type, this.id, this.number);
		}
	}

	private static final int DEFAULT_THREAD_COUNT = 16;
	private static final int MIN_THREAD_COUNT = 1;
	private static final int MAX_THREAD_COUNT = 32;

	private static final int DEFAULT_BACKLOG_SIZE = 1000;
	private static final int MIN_BACKLOG_SIZE = 10;
	private static final int MAX_BACKLOG_SIZE = 100000;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ObjectStore<?, ?> objectStore;
	private final ContentStreamStore contentStreamStore;
	private final ObjectStorageTranslator<T, V> translator;
	private final SessionFactory<S, W> sessionFactory;
	private int threadCount = Exporter.DEFAULT_THREAD_COUNT;
	private int backlogSize = Exporter.DEFAULT_BACKLOG_SIZE;

	public <C, O extends ObjectStoreOperation<C>> Exporter(ObjectStore<C, O> objectStore,
		ContentStreamStore contentStreamStore, ObjectStorageTranslator<T, V> translator,
		SessionFactory<S, W> sessionFactory) {
		super();
		this.objectStore = objectStore;
		this.contentStreamStore = contentStreamStore;
		this.translator = translator;
		this.sessionFactory = sessionFactory;
	}

	protected final ContentStreamStore getContentStreamStore() {
		return this.contentStreamStore;
	}

	protected final ObjectStorageTranslator<T, V> getTranslator() {
		return this.translator;
	}

	public final synchronized int setThreadCount(int threadCount) {
		int old = this.threadCount;
		this.threadCount = Tools.ensureBetween(Exporter.MIN_THREAD_COUNT, threadCount, Exporter.MAX_THREAD_COUNT);
		return old;
	}

	public final synchronized int getThreadCount() {
		return this.threadCount;
	}

	public final synchronized int setBacklogSize(int backlogSize) {
		int old = this.backlogSize;
		this.backlogSize = Tools.ensureBetween(Exporter.MIN_BACKLOG_SIZE, backlogSize, Exporter.MAX_BACKLOG_SIZE);
		return old;
	}

	public final synchronized int getBacklogSize() {
		return this.backlogSize;
	}

	protected final Logger getOutput() {
		return null;
	}

	protected final T getObject(W session, StoredObjectType type, String id) throws Exception {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to operate with"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide an object id"); }
		return doGetObject(session, type, id);
	}

	protected abstract T doGetObject(W session, StoredObjectType type, String id) throws Exception;

	protected final Collection<T> identifyRequirements(W session, T object) throws Exception {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to operate with"); }
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object whose requirements to identify"); }
		return doIdentifyRequirements(session, object);
	}

	protected Collection<T> doIdentifyRequirements(W session, T object) throws Exception {
		return Collections.emptyList();
	}

	protected final Collection<T> identifyDependents(W session, T object) throws Exception {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to operate with"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose dependents to identify"); }
		return doIdentifyDependents(session, object);
	}

	protected Collection<T> doIdentifyDependents(W session, T object) throws Exception {
		return Collections.emptyList();
	}

	protected abstract StoredObject<V> marshal(W session, T object) throws ExportException;

	protected final void storeSupplemental(W session, T object) throws Exception {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to operate with"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose dependents to identify"); }
		doStoreSupplemental(session, object);
	}

	protected abstract void doStoreSupplemental(W session, T object) throws Exception;

	private Long exportObject(W session, StoredObject<V> marshaled, T sourceObject) throws ExportException,
		StorageException, StoredValueEncoderException, UnsupportedObjectTypeException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to operate with"); }
		if (marshaled == null) { throw new IllegalArgumentException(
			"Must provide an object whose dependents to identify"); }

		final String id = marshaled.getId();
		final StoredObjectType type = marshaled.getType();
		final String label = String.format("%s [%s](%s)", type, marshaled.getLabel(), id);

		// First, make sure other threads don't work on this same object
		if (!this.objectStore.lockForStorage(type, id)) {
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("%s is already locked for storage", label));
			}
			return null;
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Locked %s for storage", label));
		}

		// Make sure the object hasn't already been exported
		if (this.objectStore.isStored(type, id)) {
			// Should be impossible, but still guard against it
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("%s was locked for storage by this thread, but is already stored...",
					label));
			}
			return null;
		}

		Collection<T> referenced;
		try {
			referenced = identifyRequirements(session, sourceObject);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to identify the requirements for %s", label), e);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("%s requires %d objects for successful storage", label, referenced.size()));
		}
		for (T requirement : referenced) {
			StoredObject<V> req = marshal(session, requirement);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("%s requires %s [%s](%s)", label, req.getType(), req.getLabel(),
					req.getId()));
			}
			exportObject(session, req, requirement);
		}

		final Long ret = this.objectStore.storeObject(marshaled, this.translator);
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
			referenced = identifyDependents(session, sourceObject);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to identify the dependents for %s", label), e);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("%s has %d dependent objects to store", label, referenced.size()));
		}
		for (T dependent : referenced) {
			StoredObject<V> dep = marshal(session, dependent);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("%s requires %s [%s](%s)", label, dep.getType(), dep.getLabel(),
					dep.getId()));
			}
			exportObject(session, dep, dependent);
		}

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Executing supplemental storage for %s", label));
		}
		try {
			storeSupplemental(session, sourceObject);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to execute the supplemental storage for %s", label), e);
		}

		return ret;
	}

	public final void runExport(Map<String, Object> settings) throws ExportException, StorageException {
		// We get this at the very top because if this fails, there's no point in continuing.
		final W baseSession;
		try {
			baseSession = this.sessionFactory.acquireSession();
		} catch (Exception e) {
			throw new ExportException("Failed to obtain the main export session", e);
		}

		final int threadCount;
		final int backlogSize;
		// Ensure nobody changes this under our feet
		synchronized (this) {
			threadCount = this.threadCount;
			backlogSize = this.backlogSize;
		}

		final AtomicInteger activeCounter = new AtomicInteger(0);
		final Target exitValue = new Target();
		final BlockingQueue<Target> workQueue = new ArrayBlockingQueue<Target>(backlogSize);
		final ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				final W session;
				try {
					session = Exporter.this.sessionFactory.acquireSession();
				} catch (Exception e) {
					Exporter.this.log.error("Failed to obtain a worker session", e);
					return;
				}
				if (Exporter.this.log.isDebugEnabled()) {
					Exporter.this.log.debug(String.format("Got session [%s]", session.getId()));
				}
				// final Logger output = getOutput();
				// Begin transaction
				activeCounter.incrementAndGet();
				session.begin();
				try {
					while (!Thread.interrupted()) {
						if (Exporter.this.log.isDebugEnabled()) {
							Exporter.this.log.debug("Polling the queue...");
						}
						final Target next;
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
							Exporter.this.log.info("Exiting the export polling loop");
							return;
						}

						if (Exporter.this.log.isDebugEnabled()) {
							Exporter.this.log.debug(String.format("Polled %s", next));
						}

						try {
							final T sourceObject = getObject(baseSession, next.type, next.id);
							if (sourceObject == null) {
								// No object found with that ID...
								Exporter.this.log.warn(String.format("No %s object found with ID[%s]", next.type,
									next.id));
								continue;
							}

							if (Exporter.this.log.isDebugEnabled()) {
								Exporter.this.log.debug(String.format("Exporting the source %s object with ID[%s]",
									next.type, next.id));
							}
							objectExportStarted(next.type, next.id);
							StoredObject<V> marshaled = marshal(baseSession, sourceObject);
							Long result = exportObject(baseSession, marshaled, sourceObject);
							if (marshaled != null) {
								if (Exporter.this.log.isDebugEnabled()) {
									Exporter.this.log.debug(String.format("Exported %s [%s](%s) in position %d",
										marshaled.getType(), marshaled.getLabel(), marshaled.getId(), result));
								}
								objectExportCompleted(marshaled, result);
							} else {
								if (Exporter.this.log.isDebugEnabled()) {
									Exporter.this.log.debug(String.format("%s object with ID[%s] was already exported",
										next.type, next.id));
								}
								objectSkipped(next.type, next.id);
							}
						} catch (Exception e) {
							Exporter.this.log.error(
								String.format("Failed to export %s object with ID[%s]", next.type, next.id), e);
							objectExportFailed(next.type, next.id, e);
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

		final Iterator<Target> it;
		try {
			it = findExportResults(baseSession.getWrapped(), settings);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to obtain the export results with settings: %s", settings),
				e);
		}
		try {
			int counter = 0;
			// 1: run the query for the given predicate
			exportStarted(settings);
			// 2: iterate over the results, gathering up the object IDs
			while (it.hasNext()) {
				if (this.log.isTraceEnabled()) {
					this.log.trace("Retrieving the next target from search");
				}
				Target id = it.next();
				try {
					workQueue.put(id);
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
				List<Target> remaining = new ArrayList<Target>();
				workQueue.drainTo(remaining);
				for (Target v : remaining) {
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
			baseSession.close();

			Map<StoredObjectType, Integer> summary = Collections.emptyMap();
			try {
				summary = this.objectStore.getStoredObjectTypes();
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

	protected abstract Iterator<Target> findExportResults(S session, Map<String, Object> settings) throws Exception;

}