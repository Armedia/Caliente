/**
 *
 */

package com.delta.cmsmf.engine;

import java.util.concurrent.Callable;

import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.com.DfClientX;
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

	private class Worker implements Callable<Boolean> {

		private final DctmSessionManager manager;
		private final IDfId id;
		private final CmsObjectStore store;

		private Worker(DctmSessionManager manager, CmsObjectStore store, IDfId id) {
			this.manager = manager;
			this.id = id;
			this.store = store;
		}

		@Override
		public Boolean call() throws Exception {
			IDfSession session = this.manager.acquireSession();
			try {
				IDfPersistentObject dfObj = session.getObject(this.id);
				return this.store.persistDfObject(dfObj);
			} finally {
				this.manager.releaseSession(session);
			}
		}

	}

	public void doExport(CmsObjectStore objectStore, DctmSessionManager sessionManager) throws DfException,
		CMSMFException {

		IDfSession session = sessionManager.acquireSession();
		try {
			// 1: run the query for the given predicate
			String dql = "select r_object_id from dm_sysobject where folder('/CMSMFTests', DESCEND)";
			IDfQuery query = new DfClientX().getQuery();
			query.setDQL(dql);

			// 2: iterate over the results, gathering up the object IDs
			IDfCollection results = query.execute(session, IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					final IDfId id = results.getId("r_object_id");
					// TODO: Multi-thread this
					try {
						new Worker(sessionManager, objectStore, id).call();
					} catch (Exception e) {
						// What happened?
						e.printStackTrace();
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