/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.tools.dfc.pool;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.fc.tools.RegistryPasswordUtils;

public class DfcSessionFactory implements PooledObjectFactory<IDfSession> {

	public static final String DOCBASE = "docbase";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";

	public static enum DfCache {
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

	public static final IDfClient CLIENT;

	static {
		try {
			CLIENT = DfClient.getLocalClient();
		} catch (DfException e) {
			throw new RuntimeException("Failed to initialize the local DFC client", e);
		}
		if (DfcSessionFactory.CLIENT == null) {
			throw new RuntimeException(
				"No local client was established.  You may want to check the installation of Documentum or this application on this machine.");
		}
	}

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final IDfLoginInfo loginInfo = new DfLoginInfo();
	private final IDfSessionManager sessionManager = DfcSessionFactory.CLIENT.newSessionManager();
	private final String docbase;

	public DfcSessionFactory(CfgTools settings) throws DfException {
		this(settings.getString(DfcSessionFactory.USERNAME), settings.getString(DfcSessionFactory.PASSWORD),
			settings.getString(DfcSessionFactory.DOCBASE));
	}

	public DfcSessionFactory(String username, String password, String docbase) throws DfException {

		this.docbase = docbase;
		if (username != null) {
			this.loginInfo.setUser(username);
		}

		if (password != null) {
			String passTmp = password;
			try {
				passTmp = RegistryPasswordUtils.decrypt(password);
				if (this.log.isInfoEnabled()) {
					this.log.info("Password decrypted successfully");
				}
			} catch (Throwable t) {
				// Not encrypted, use literal
				passTmp = password;
				if (this.log.isInfoEnabled()) {
					this.log.info("Password decryption failed, using as literal");
				}
			}
			this.loginInfo.setPassword(passTmp);
		}
		this.loginInfo.setDomain(null);
		this.sessionManager.setIdentity(docbase, this.loginInfo);
	}

	protected final void flushCaches(IDfSession session) throws DfException {
		final String sessionId = DfcSessionPool.getId(session);
		if (this.log.isTraceEnabled()) {
			this.log.trace("Flushing all the session caches for session [{}]", sessionId);
		}
		for (DfCache cache : DfCache.values()) {
			this.log.trace("Flushing the [{}] cache for session [{}]", cache.name(), sessionId);
			try {
				cache.flush(session, null);
			} catch (DfException e) {
				if (this.log.isTraceEnabled()) {
					this.log.error("Exception caught flushing the [{}] cache for session [{}]", cache.name(), sessionId,
						e);
				}
			}
		}
		session.flushCache(true);
	}

	@Override
	public PooledObject<IDfSession> makeObject() throws DfException {
		IDfSession session = this.sessionManager.newSession(this.docbase);
		if (this.log.isDebugEnabled()) {
			this.log.debug("Creating a new session to [{}]: [{}]", this.docbase, session.getSessionId());
		}
		return new DefaultPooledObject<>(session);
	}

	@Override
	public void destroyObject(PooledObject<IDfSession> obj) throws DfException {
		final IDfSession session = obj.getObject();
		if (session == null) { return; }
		if (this.log.isDebugEnabled()) {
			this.log.debug("Closing a session to [{}]: [{}]", this.docbase, DfcSessionPool.getId(session));
		}
		try {
			if (session.isTransactionActive()) {
				if (this.log.isDebugEnabled()) {
					this.log.debug("Session to [{}] with ID [{}] had an open transaction, aborting it", this.docbase,
						DfcSessionPool.getId(session));
				}
				session.abortTrans();
			}
		} finally {
			try {
				session.disconnect();
			} catch (DfException e) {
				// Safely ignore
			}
		}
	}

	@Override
	public boolean validateObject(PooledObject<IDfSession> obj) {
		final IDfSession session = obj.getObject();
		if (session == null) { return false; }
		if (!session.isConnected()) { return false; }
		try {
			// Make sure our transaction stack is clear... if there's an error here,
			// it means we need a new session and this one needs destroying...
			session.beginTrans();
			session.abortTrans();
		} catch (DfException e) {
			// Not valid...
			return false;
		}
		// None of our validation tests failed, so we mark the session as valid
		return true;
	}

	@Override
	public void activateObject(PooledObject<IDfSession> obj) throws DfException {
		if (obj == null) { return; }
		flushCaches(obj.getObject());
	}

	@Override
	public void passivateObject(PooledObject<IDfSession> obj) throws DfException {
		if (obj == null) { return; }
		flushCaches(obj.getObject());
	}
}