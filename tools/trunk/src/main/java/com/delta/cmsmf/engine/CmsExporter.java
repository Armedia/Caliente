/**
 *
 */

package com.delta.cmsmf.engine;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

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
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsExporter {

	private Logger log = Logger.getLogger(getClass());

	public static final int DEFAULT_BACKLOG_SIZE = 100;
	public static final int MAX_BACKLOG_SIZE = 1000;
	public static final int DEFAULT_THREAD_COUNT = 4;
	public static final int MAX_THREAD_COUNT = 32;

	private final int backlogSize;
	private final int threadCount;

	public CmsExporter() {
		this(CmsExporter.DEFAULT_THREAD_COUNT);
	}

	public CmsExporter(int threadCount) {
		this(threadCount, CmsExporter.DEFAULT_BACKLOG_SIZE);
	}

	public CmsExporter(int threadCount, int backlogSize) {
		if (threadCount <= 0) {
			threadCount = 1;
		}
		if (threadCount > CmsExporter.MAX_THREAD_COUNT) {
			threadCount = CmsExporter.MAX_THREAD_COUNT;
		}
		if (backlogSize <= 0) {
			backlogSize = 10;
		}
		if (backlogSize > CmsImporter.MAX_BACKLOG_SIZE) {
			backlogSize = CmsImporter.MAX_BACKLOG_SIZE;
		}
		this.threadCount = threadCount;
		this.backlogSize = backlogSize;
	}

	public int getThreadCount() {
		return this.threadCount;
	}

	public void doExport(final CmsObjectStore objectStore, final DctmSessionManager sessionManager,
		final String dqlPredicate) throws DfException, CMSMFException {

		IDfSession session = sessionManager.acquireSession();

		final AtomicInteger activeCounter = new AtomicInteger(0);
		final String exitFlag = String.format("%s[%s]", toString(), UUID.randomUUID().toString());
		final IDfValue exitValue = DfValueFactory.newStringValue(exitFlag);
		final BlockingQueue<IDfValue> workQueue = new ArrayBlockingQueue<IDfValue>(this.threadCount * this.backlogSize);
		ExecutorService executor = new ThreadPoolExecutor(this.threadCount, this.threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				IDfSession session = sessionManager.acquireSession();
				activeCounter.incrementAndGet();
				try {
					while (true) {
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

						if (exitFlag.equals(next.asString())) {
							// Work complete
							CmsExporter.this.log.info("Exiting the export polling loop");
							return;
						}

						final IDfId id = next.asId();

						if (CmsExporter.this.log.isDebugEnabled()) {
							CmsExporter.this.log.debug(String.format("Polled ID %s", id.getId()));
						}

						if (CmsExporter.this.log.isDebugEnabled()) {
							CmsExporter.this.log.debug(String.format("Got IDfSession [%s]",
								DfUtils.getSessionId(session)));
						}
						try {
							IDfPersistentObject dfObj = session.getObject(id);
							if (CmsExporter.this.log.isDebugEnabled()) {
								CmsExporter.this.log.debug(String.format("Retrieved [%s] object with id [%s]", dfObj
									.getType().getName(), dfObj.getObjectId().getId()));
							}
							objectStore.persistDfObject(dfObj);
							if (CmsExporter.this.log.isDebugEnabled()) {
								CmsExporter.this.log.debug(String.format("Persisted [%s] object with id [%s]", dfObj
									.getType().getName(), dfObj.getObjectId().getId()));
							}
						} catch (Throwable t) {
							// Log the error, move on
							CmsExporter.this.log.error(
								String.format("Exception caught processing object with ID [%s]", id.getId()), t);
						}
					}
				} finally {
					activeCounter.decrementAndGet();
					sessionManager.releaseSession(session);
				}
			}
		};

		// Fire off the workers
		for (int i = 0; i < this.threadCount; i++) {
			executor.submit(worker);
		}
		executor.shutdown();

		try {
			// 1: run the query for the given predicate
			final String dql = String.format("select r_object_id %s", dqlPredicate);
			IDfCollection results = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			try {
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
				for (int i = 0; i < this.threadCount; i++) {
					try {
						workQueue.put(exitValue);
					} catch (InterruptedException e) {
						// Here we have a problem: we're timing out while adding the exit values...
						this.log.warn("Interrupted while attempting to request executor thread termination", e);
						break;
					}
				}
				int pending = activeCounter.get();
				if (pending > 0) {
					try {
						this.log.info(String
							.format("Waiting for pending workers to terminate (maximum 5 minutes, %d pending workers)",
								pending));
						executor.awaitTermination(5, TimeUnit.MINUTES);
					} catch (InterruptedException e) {
						this.log.warn("Interrupted while waiting for normal executor termination", e);
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
					}
				}
				DfUtils.closeQuietly(results);
			}
		} finally {
			sessionManager.releaseSession(session);
		}
	}
}