/**
 *
 */

package com.delta.cmsmf.cms.pool;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.DfServiceException;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class DctmConnectionPool {

	private static final Logger LOG = Logger.getLogger(DctmConnectionPool.class);
	private static final IDfClient CLIENT;

	static {
		try {
			CLIENT = DfClient.getLocalClient();
		} catch (DfException e) {
			throw new RuntimeException("Failed to initialize the local DFC client", e);
		}
		if (DctmConnectionPool.CLIENT == null) { throw new RuntimeException(
			"No local client was established.  You may want to check the installation of Documentum or this application on this machine."); }
	}

	private final String docbase;
	private final IDfLoginInfo loginInfo;
	private final IDfSessionManager sessionManager;
	private final GenericObjectPool<IDfSession> pool;

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

	private final PoolableObjectFactory<IDfSession> FACTORY = new PoolableObjectFactory<IDfSession>() {

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

	public DctmConnectionPool() {
		this(null, null, null);
	}

	public DctmConnectionPool(String docbase) {
		this(docbase, null, null);
	}

	public DctmConnectionPool(String docbase, String username, String password) {
		this.docbase = docbase;
		this.loginInfo = new DfLoginInfo();
		if (username != null) {
			this.loginInfo.setUser(username);
		}
		if (password != null) {
			String passTmp = password;
			try {
				passTmp = RegistryPasswordUtils.decrypt(password);
				if (DctmConnectionPool.LOG.isEnabledFor(Level.INFO)) {
					DctmConnectionPool.LOG.info(String.format("Password decrypted successfully"));
				}
			} catch (Throwable t) {
				// Not encrypted, use literal
				passTmp = password;
				if (DctmConnectionPool.LOG.isEnabledFor(Level.INFO)) {
					DctmConnectionPool.LOG.info(String.format("Password decryption failed, using as literal"));
				}
			}
			this.loginInfo.setPassword(passTmp);
		}
		this.loginInfo.setDomain(null);
		this.sessionManager = DctmConnectionPool.CLIENT.newSessionManager();
		try {
			this.sessionManager.setIdentity(this.docbase, this.loginInfo);
		} catch (DfServiceException e) {
			throw new RuntimeException("Failed to set the identity for the session manager", e);
		}

		this.pool = new GenericObjectPool<IDfSession>(this.FACTORY);
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
		config.timeBetweenEvictionRunsMillis = 30000;
		config.minEvictableIdleTimeMillis = 45000;
		this.pool.setConfig(config);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				close();
			}
		});
	}

	public IDfSession acquireSession() {
		try {
			return this.pool.borrowObject();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create a new Documentum session", e);
		}
	}

	public void releaseSession(IDfSession session) {
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