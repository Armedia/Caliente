/**
 *
 */

package com.delta.cmsmf.engine;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

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

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsExporter {

	private Logger log = Logger.getLogger(getClass());

	public void doExport(final CmsObjectStore objectStore, final DctmSessionManager sessionManager,
		final String dqlPredicate) throws DfException, CMSMFException {

		IDfSession session = sessionManager.acquireSession();

		// TODO: Make capacity configurable
		final int threadCount = 10;
		final BlockingQueue<IDfId> workQueue = new ArrayBlockingQueue<IDfId>(threadCount);
		ExecutorService executor = new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				IDfSession session = sessionManager.acquireSession();
				try {
					while (true) {
						if (CmsExporter.this.log.isDebugEnabled()) {
							CmsExporter.this.log.debug("Polling the queue...");
						}
						final IDfId id;
						try {
							id = workQueue.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}

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
					sessionManager.releaseSession(session);
				}
			}
		};

		// Fire off the workers
		for (int i = 0; i < threadCount; i++) {
			executor.submit(worker);
		}

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
					final IDfId id = results.getId("r_object_id");
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
			} finally {
				DfUtils.closeQuietly(results);
			}
		} finally {
			sessionManager.releaseSession(session);
		}
	}
}