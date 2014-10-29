/**
 *
 */

package com.armedia.cmf.engine.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class Exporter<T, V, S, W extends SessionWrapper<S>> implements ExportEngineListener {

	private static final int DEFAULT_THREAD_COUNT = 16;
	private static final int MIN_THREAD_COUNT = 1;
	private static final int MAX_THREAD_COUNT = 32;

	private static final int DEFAULT_BACKLOG_SIZE = 1000;
	private static final int MIN_BACKLOG_SIZE = 10;
	private static final int MAX_BACKLOG_SIZE = 100000;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ObjectStore objectStore;
	private final ContentStreamStore contentStreamStore;
	private final ObjectStorageTranslator<T, V> translator;
	private final SessionFactory<S, W> sessionFactory;
	private int threadCount = Exporter.DEFAULT_THREAD_COUNT;
	private int backlogSize = Exporter.DEFAULT_BACKLOG_SIZE;

	public Exporter(ObjectStore objectStore, ContentStreamStore contentStreamStore,
		ObjectStorageTranslator<T, V> translator, SessionFactory<S, W> sessionFactory) {
		super();
		this.objectStore = objectStore;
		this.contentStreamStore = contentStreamStore;
		this.translator = translator;
		this.sessionFactory = sessionFactory;
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

	protected final ExportResult processObject(T object) throws ExportException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to load"); }
		return null;
	}

	protected abstract Iterator<String> findExportResults(S session, Map<String, Object> settings) throws Exception;

	protected abstract StoredObject<V> marshal(T object) throws ExportException;

	protected abstract T getObjectById(String id) throws Exception;

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
		final String exitValue = String.format("%s[%s]", toString(), UUID.randomUUID().toString());
		final BlockingQueue<String> workQueue = new ArrayBlockingQueue<String>(backlogSize);
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
				final Logger output = getOutput();
				// Begin transaction
				activeCounter.incrementAndGet();
				session.begin();
				try {
					while (!Thread.interrupted()) {
						if (Exporter.this.log.isDebugEnabled()) {
							Exporter.this.log.debug("Polling the queue...");
						}
						final String next;
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
							Exporter.this.log.debug(String.format("Polled ID %s", next));
						}

						final T sourceObject;
						final StoredObjectType type;
						try {
							sourceObject = getObjectById(next);
							if (sourceObject == null) {
								// No object found with that ID...
								Exporter.this.log.warn(String.format("No object found with ID[%s]", next));
								continue;
							}

							type = Exporter.this.translator.decodeObjectType(sourceObject);
							if (Exporter.this.log.isDebugEnabled()) {
								Exporter.this.log.debug(String.format("Retrieved %s object with id [%s]", type.name(),
									next));
							}
						} catch (Exception e) {
							// Failed to identify the object being exported
							Exporter.this.log.error(
								String.format("Failed to identify the exported object with ID [%s]", next), e);
							continue;
						}

						try {
							objectExportStarted(type, next);

							StoredObject<V> object = marshal(sourceObject);
							/*
							DctmExportContext ctx = new DctmExportContext(next, session, Exporter.this.objectStore,
								Exporter.this.contentStreamStore, output, Exporter.this.exportListener);
							 */
							Long objectNumber = Exporter.this.objectStore.storeObject(object, Exporter.this.translator);
							if (objectNumber != null) {
								objectExportCompleted(object, objectNumber);
							} else {
								objectSkipped(type, next);
							}
							if (Exporter.this.log.isDebugEnabled()) {
								Exporter.this.log.debug(String.format("Persisted %s object with id [%s]", type, next));
							}
						} catch (Exception e) {
							// Log the error, move on
							objectExportFailed(type, next, e);
							Exporter.this.log.error(
								String.format("Exception caught processing object with ID [%s]", next), e);
						} finally {
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

		final Iterator<String> it;
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
					this.log.trace("Retrieving the next ID from search");
				}
				String id = it.next();
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
}