/**
 *
 */

package com.delta.cmsmf.runtime;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.mainEngine.CLIParam;
import com.delta.cmsmf.mainEngine.CMSMFLauncher;
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

	private static final String DOCBASE;
	private static final IDfClient CLIENT;
	private static final IDfLoginInfo LOGIN_INFO;
	private static final IDfSessionManager SESSION_MANAGER;
	private static final GenericObjectPool<IDfSession> POOL;

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

	private static final PoolableObjectFactory<IDfSession> FACTORY = new PoolableObjectFactory<IDfSession>() {

		@Override
		public IDfSession makeObject() throws Exception {
			IDfSession session = DctmConnectionPool.SESSION_MANAGER.newSession(DctmConnectionPool.DOCBASE);
			if (DctmConnectionPool.LOG.isDebugEnabled()) {
				DctmConnectionPool.LOG.debug(String.format("Creating a new session to [%s]: [%s]",
					DctmConnectionPool.DOCBASE, session.getSessionId()));
			}
			return session;
		}

		@Override
		public void destroyObject(IDfSession obj) throws Exception {
			if (obj == null) { return; }
			if (DctmConnectionPool.LOG.isDebugEnabled()) {
				DctmConnectionPool.LOG.debug(String.format("Closing a session to [%s]: [%s]",
					DctmConnectionPool.DOCBASE, DctmConnectionPool.getId(obj)));
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

	static {
		DOCBASE = CMSMFLauncher.getParameter(CLIParam.docbase);
		final String username = CMSMFLauncher.getParameter(CLIParam.user);
		final String password = CMSMFLauncher.getParameter(CLIParam.password);

		try {
			CLIENT = DfClient.getLocalClient();
		} catch (DfException e) {
			throw new RuntimeException("Failed to initialize the local DFC client", e);
		}
		if (DctmConnectionPool.CLIENT == null) { throw new RuntimeException(
			"No local client was established.  You may want to check the installation of Documentum or this application on this machine."); }
		LOGIN_INFO = new DfLoginInfo();
		if (username != null) {
			DctmConnectionPool.LOGIN_INFO.setUser(username);
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
			DctmConnectionPool.LOGIN_INFO.setPassword(passTmp);
		}
		DctmConnectionPool.LOGIN_INFO.setDomain(null);
		SESSION_MANAGER = DctmConnectionPool.CLIENT.newSessionManager();
		try {
			DctmConnectionPool.SESSION_MANAGER.setIdentity(DctmConnectionPool.DOCBASE, DctmConnectionPool.LOGIN_INFO);
		} catch (DfServiceException e) {
			throw new RuntimeException("Failed to set the identity for the session manager", e);
		}

		POOL = new GenericObjectPool<IDfSession>(DctmConnectionPool.FACTORY);
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
		DctmConnectionPool.POOL.setConfig(config);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				DctmConnectionPool.close();
			}
		});
	}

	private DctmConnectionPool() {
	}

	public static IDfSession acquireSession() {
		try {
			return DctmConnectionPool.POOL.borrowObject();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create a new Documentum session", e);
		}
	}

	public static void releaseSession(IDfSession session) {
		if (session == null) { return; }
		try {
			DctmConnectionPool.POOL.returnObject(session);
		} catch (Exception e) {
			DctmConnectionPool.LOG
				.warn(
					String.format("Exception caught returning session [%s] to the pool",
						DctmConnectionPool.getId(session)), e);
		}
	}

	public static void close() {
		try {
			DctmConnectionPool.POOL.close();
		} catch (Exception e) {
			DctmConnectionPool.LOG.warn("Exception caught closing the pool instance", e);
		}
	}
}