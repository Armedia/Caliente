/**
 *
 */

package com.armedia.cmf.documentum.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * @author diego
 *
 */
public class DctmSessionFactory extends SessionFactory<IDfSession> {

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

	private static final Logger LOG = LoggerFactory.getLogger(DctmSessionFactory.class);
	private static final IDfClient CLIENT;

	static {
		try {
			CLIENT = DfClient.getLocalClient();
		} catch (DfException e) {
			throw new RuntimeException("Failed to initialize the local DFC client", e);
		}
		if (DctmSessionFactory.CLIENT == null) { throw new RuntimeException(
			"No local client was established.  You may want to check the installation of Documentum or this application on this machine."); }
	}

	private static String getId(IDfSession obj) {
		if (obj != null) {
			try {
				return obj.getSessionId();
			} catch (DfException e) {
				if (DctmSessionFactory.LOG.isTraceEnabled()) {
					DctmSessionFactory.LOG.trace("Exception caught determining the ID of a session", e);
				}
			}
		}
		return "(unknown)";
	}

	private final IDfLoginInfo loginInfo = new DfLoginInfo();
	private final IDfSessionManager sessionManager = DctmSessionFactory.CLIENT.newSessionManager();
	private String docbase = null;

	@Override
	protected void doInit(CfgTools settings) throws Exception {
		super.doInit(settings);

		final String docbase = settings.getString("docbase");
		final String username = settings.getString("username");
		final String password = settings.getString("password");

		this.docbase = docbase;
		if (username != null) {
			this.loginInfo.setUser(username);
		}

		if (password != null) {
			String passTmp = password;
			try {
				passTmp = RegistryPasswordUtils.decrypt(password);
				if (DctmSessionFactory.LOG.isInfoEnabled()) {
					DctmSessionFactory.LOG.info(String.format("Password decrypted successfully"));
				}
			} catch (Throwable t) {
				// Not encrypted, use literal
				passTmp = password;
				if (DctmSessionFactory.LOG.isInfoEnabled()) {
					DctmSessionFactory.LOG.info(String.format("Password decryption failed, using as literal"));
				}
			}
			this.loginInfo.setPassword(passTmp);
		}
		this.loginInfo.setDomain(null);
		this.sessionManager.setIdentity(docbase, this.loginInfo);
	}

	private void flushCaches(IDfSession session) throws DfException {
		final String sessionId = DfUtils.getSessionId(session);
		if (DctmSessionFactory.LOG.isTraceEnabled()) {
			DctmSessionFactory.LOG.trace(String.format("Flushing all the session caches for session [%s]", sessionId));
		}
		for (DfCache cache : DfCache.values()) {
			if (DctmSessionFactory.LOG.isTraceEnabled()) {
				DctmSessionFactory.LOG.trace(String.format("Flushing the [%s] cache for session [%s]", cache.name(),
					sessionId));
			}
			try {
				cache.flush(session, null);
			} catch (DfException e) {
				if (DctmSessionFactory.LOG.isTraceEnabled()) {
					DctmSessionFactory.LOG.error(String.format(
						"Exception caught flushing the [%s] cache for session [%s]", cache.name(), sessionId), e);
				}
			}
		}
		session.flushCache(true);
	}

	@Override
	public IDfSession makeObject() throws Exception {
		IDfSession session = this.sessionManager.newSession(this.docbase);
		if (DctmSessionFactory.LOG.isDebugEnabled()) {
			DctmSessionFactory.LOG.debug(String.format("Creating a new session to [%s]: [%s]", this.docbase,
				session.getSessionId()));
		}
		return session;
	}

	@Override
	public void destroyObject(IDfSession obj) throws Exception {
		if (obj == null) { return; }
		if (DctmSessionFactory.LOG.isDebugEnabled()) {
			DctmSessionFactory.LOG.debug(String.format("Closing a session to [%s]: [%s]", this.docbase,
				DctmSessionFactory.getId(obj)));
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
		flushCaches(obj);
	}

	@Override
	public void passivateObject(IDfSession obj) throws Exception {
		if (obj == null) { return; }
		flushCaches(obj);
	}

	@Override
	protected DctmSessionWrapper newWrapper(IDfSession session) throws Exception {
		return new DctmSessionWrapper(this, session);
	}
}