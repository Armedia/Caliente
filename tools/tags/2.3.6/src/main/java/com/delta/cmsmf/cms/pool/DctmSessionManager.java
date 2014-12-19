/**
 *
 */

package com.delta.cmsmf.cms.pool;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import com.delta.cmsmf.utils.DfUtils;
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
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmSessionManager {

	private static enum DfCache {
		//
		querycache,
		aclcache,
		groupcache,
		// ddcache,
		// registrycache,
		persistentcache,
		persistentobjcache;

		public void flush(IDfSession session, String cacheKey) throws DfException {
			session.flush(name(), cacheKey);
		}
	}

	private static final Logger LOG = Logger.getLogger(DctmSessionManager.class);
	private static final IDfClient CLIENT;

	static {
		try {
			CLIENT = DfClient.getLocalClient();
		} catch (DfException e) {
			throw new RuntimeException("Failed to initialize the local DFC client", e);
		}
		if (DctmSessionManager.CLIENT == null) { throw new RuntimeException(
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
				if (DctmSessionManager.LOG.isTraceEnabled()) {
					DctmSessionManager.LOG.trace("Exception caught determining the ID of a session", e);
				}
			}
		}
		return "(unknown)";
	}

	private final PoolableObjectFactory<IDfSession> FACTORY = new PoolableObjectFactory<IDfSession>() {

		private void flushCaches(IDfSession session) throws DfException {
			final String sessionId = DfUtils.getSessionId(session);
			if (DctmSessionManager.LOG.isDebugEnabled()) {
				DctmSessionManager.LOG.debug(String.format("Flushing all the session caches for session [%s]",
					sessionId));
			}
			for (DfCache cache : DfCache.values()) {
				if (DctmSessionManager.LOG.isTraceEnabled()) {
					DctmSessionManager.LOG.trace(String.format("Flushing the [%s] cache for session [%s]",
						cache.name(), sessionId));
				}
				try {
					cache.flush(session, null);
				} catch (DfException e) {
					if (DctmSessionManager.LOG.isDebugEnabled()) {
						DctmSessionManager.LOG.error(String.format(
							"Exception caught flushing the [%s] cache for session [%s]", cache.name(), sessionId), e);
					}
				}
			}
			session.flushCache(true);
		}

		@Override
		public IDfSession makeObject() throws Exception {
			IDfSession session = DctmSessionManager.this.sessionManager.newSession(DctmSessionManager.this.docbase);
			if (DctmSessionManager.LOG.isDebugEnabled()) {
				DctmSessionManager.LOG.debug(String.format("Creating a new session to [%s]: [%s]",
					DctmSessionManager.this.docbase, session.getSessionId()));
			}
			return session;
		}

		@Override
		public void destroyObject(IDfSession obj) throws Exception {
			if (obj == null) { return; }
			if (DctmSessionManager.LOG.isDebugEnabled()) {
				DctmSessionManager.LOG.debug(String.format("Closing a session to [%s]: [%s]",
					DctmSessionManager.this.docbase, DctmSessionManager.getId(obj)));
			}
			try {
				cleanupObject(obj);
			} catch (Exception e) {
				if (DctmSessionManager.LOG.isDebugEnabled()) {
					DctmSessionManager.LOG.warn(
						String.format("Exception caught cleaning up session [%s] to [%s]",
							DctmSessionManager.getId(obj), DctmSessionManager.this.docbase), e);
				}
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

		private void cleanupObject(IDfSession obj) throws Exception {
			if (obj == null) { return; }
			if (obj.isTransactionActive()) {
				DctmSessionManager.LOG.warn(String.format(
					"Session [%s] to [%s] was left with an open transaction which will be aborted",
					DctmSessionManager.getId(obj), DctmSessionManager.this.docbase));
				obj.abortTrans();
			}
			flushCaches(obj);
		}

		@Override
		public void activateObject(IDfSession obj) throws Exception {
			cleanupObject(obj);
		}

		@Override
		public void passivateObject(IDfSession obj) throws Exception {
			cleanupObject(obj);
		}
	};

	public DctmSessionManager() {
		this(null, null, null);
	}

	public DctmSessionManager(String docbase) {
		this(docbase, null, null);
	}

	public DctmSessionManager(String docbase, String username, String password) {
		this.docbase = docbase;
		this.loginInfo = new DfLoginInfo();
		if (username != null) {
			this.loginInfo.setUser(username);
		}
		if (password != null) {
			String passTmp = password;
			try {
				passTmp = RegistryPasswordUtils.decrypt(password);
				if (DctmSessionManager.LOG.isInfoEnabled()) {
					DctmSessionManager.LOG.info(String.format("Password decrypted successfully"));
				}
			} catch (Throwable t) {
				// Not encrypted, use literal
				passTmp = password;
				if (DctmSessionManager.LOG.isInfoEnabled()) {
					DctmSessionManager.LOG.info(String.format("Password decryption failed, using as literal"));
				}
			}
			this.loginInfo.setPassword(passTmp);
		}
		this.loginInfo.setDomain(null);
		this.sessionManager = DctmSessionManager.CLIENT.newSessionManager();
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
			DctmSessionManager.LOG
				.warn(
					String.format("Exception caught returning session [%s] to the pool",
						DctmSessionManager.getId(session)), e);
		}
	}

	public void close() {
		try {
			this.pool.close();
		} catch (Exception e) {
			DctmSessionManager.LOG.warn("Exception caught closing the pool instance", e);
		}
	}
}