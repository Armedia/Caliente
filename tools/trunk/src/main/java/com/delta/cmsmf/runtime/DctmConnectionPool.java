/**
 *
 */

package com.delta.cmsmf.runtime;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;

/**
 * @author diego
 *
 */
public class DctmConnectionPool {

	private static final Logger LOG = Logger.getLogger(DctmConnectionPool.class);

	private final String docbase;
	private final IDfClient client;
	private final IDfLoginInfo loginInfo;
	private final IDfSessionManager sessionManager;

	private static String getId(IDfSession obj) {
		if (obj != null) {
			try {
				return obj.getSessionId();
			} catch (DfException e) {
				if (DctmConnectionPool.LOG.isTraceEnabled()) {
					DctmConnectionPool.LOG.trace("Exception caught determining the ID of a session", e);
				}
			}
		}
		return "(unknown)";
	}

	private final PoolableObjectFactory<IDfSession> factory = new PoolableObjectFactory<IDfSession>() {

		@Override
		public IDfSession makeObject() throws Exception {
			IDfSession session = DctmConnectionPool.this.sessionManager.newSession(DctmConnectionPool.this.docbase);
			if (DctmConnectionPool.LOG.isDebugEnabled()) {
				DctmConnectionPool.LOG.debug(String.format("Creating a new session to [%s]: [%s]",
					DctmConnectionPool.this.docbase, session.getSessionId()));
			}
			return session;
		}

		@Override
		public void destroyObject(IDfSession obj) throws Exception {
			if (obj == null) { return; }
			if (DctmConnectionPool.LOG.isDebugEnabled()) {
				DctmConnectionPool.LOG.debug(String.format("Closing a session to [%s]: [%s]",
					DctmConnectionPool.this.docbase, DctmConnectionPool.getId(obj)));
			}
			try {
				obj.disconnect();
			} catch (DfException e) {
				// Safely ignore
			}
		}

		@Override
		public boolean validateObject(IDfSession obj) {
			if (obj == null) { return false; }
			return obj.isConnected();
		}

		@Override
		public void activateObject(IDfSession obj) throws Exception {
			if (obj == null) { return; }
			// do nothing
		}

		@Override
		public void passivateObject(IDfSession obj) throws Exception {
			if (obj == null) { return; }
			// do nothing
		}
	};

	private final GenericObjectPool<IDfSession> pool;

	public DctmConnectionPool(String docbase, String username, String password) throws DfException {
		this.client = DfClient.getLocalClient();
		if (this.client == null) { throw new DfException(
			"No local client was established.  You may want to check the installation of Documentum or this application on this machine."); }
		this.docbase = docbase;
		this.loginInfo = new DfLoginInfo(username, password);
		this.sessionManager = this.client.newSessionManager();
		this.sessionManager.setIdentity(docbase, this.loginInfo);
		GenericObjectPool<IDfSession> pool = new GenericObjectPool<IDfSession>(this.factory);
		GenericObjectPool.Config config = new GenericObjectPool.Config();
		config.lifo = true;
		config.maxActive = -1;
		config.maxWait = 0; // wait indefinitely
		config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
		config.minIdle = 1;
		// Make sure garbage gets collected quickly
		config.testOnBorrow = true;
		config.testOnReturn = true;
		config.maxIdle = Math.max(config.minIdle, config.maxActive / 2);
		pool.setConfig(config);
		this.pool = pool;
	}

	public IDfSession getConnection() {
		try {
			return this.pool.borrowObject();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create a new Documentum session", e);
		}
	}

	public void release(IDfSession session) {
		if (session == null) { return; }
		try {
			this.pool.returnObject(session);
		} catch (Exception e) {
			DctmConnectionPool.LOG
				.warn(
					String.format("Exception caught returning session [%s] to the pool",
						DctmConnectionPool.getId(session)), e);
		}
	}

	public void close() {
		try {
			this.pool.close();
		} catch (Exception e) {
			DctmConnectionPool.LOG.warn("Exception caught closing the pool instance", e);
		}
	}
}