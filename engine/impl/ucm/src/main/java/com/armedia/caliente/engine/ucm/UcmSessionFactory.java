/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.ucm;

import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.common.SessionFactory;
import com.armedia.caliente.engine.common.SessionFactoryException;
import com.armedia.caliente.engine.ucm.UcmSessionSetting.SSLMode;
import com.armedia.caliente.engine.ucm.model.UcmModel;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.KeyStoreTools;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.EncodedString;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.protocol.intradoc.IntradocClient;
import oracle.stellent.ridc.protocol.intradoc.IntradocClientConfig;

public class UcmSessionFactory extends SessionFactory<UcmSession> {

	public static final int MAX_PING_TIME = (int) TimeUnit.MINUTES.toSeconds(15);
	public static final long MAX_SOCKET_TIMEOUT = TimeUnit.MINUTES.toMillis(15);

	private class IdcContextSeed {
		private final String user;
		private final EncodedString password;

		public IdcContextSeed(String user, EncodedString password) {
			this.user = user;
			this.password = password;
		}

		public IdcContext newInstance() {
			if (this.password == null) { return new IdcContext(this.user); }
			try {
				String pass = (this.password != null ? this.password.decode().toString() : StringUtils.EMPTY);
				return new IdcContext(this.user, pass);
			} catch (Exception e) {
				throw new RuntimeException("Failed to decrypt the password", e);
			}
		}
	}

	private final IdcClientManager manager;
	private final String host;
	private final int port;
	private final UcmSessionSetting.SSLMode sslMode;
	private final String url;
	private final String trustStore;
	private final String trustStorePassword;
	private final String clientStore;
	private final String clientStorePassword;
	private final String clientCertAlias;
	private final String clientCertPassword;
	private final String sslAlgorithm;
	private final Long socketTimeout;
	private final IdcContextSeed context;
	private final int minPingTime;

	private final UcmModel model;

	public UcmSessionFactory(CfgTools settings, CmfCrypt crypto) throws Exception {
		super(settings, crypto);

		this.manager = new IdcClientManager();
		this.minPingTime = Tools.ensureBetween(0, settings.getInteger(UcmSessionSetting.MIN_PING_TIME),
			UcmSessionFactory.MAX_PING_TIME);
		this.host = settings.getString(UcmSessionSetting.HOST);
		this.port = settings.getInteger(UcmSessionSetting.PORT);
		if ((this.port <= 0) || (this.port > 0xffff)) {
			throw new Exception(String.format("Port number must be a number between 1 and 65535 (got %d)", this.port));
		}

		Integer socketTimeout = settings.getInteger(UcmSessionSetting.SOCKET_TIMEOUT);
		if (socketTimeout != null) {
			this.socketTimeout = Tools.ensureBetween( //
				0L, //
				TimeUnit.SECONDS.toMillis(socketTimeout.longValue()), //
				UcmSessionFactory.MAX_SOCKET_TIMEOUT //
			);
		} else {
			this.socketTimeout = null;
		}

		String userName = settings.getString(UcmSessionSetting.USER);
		EncodedString password = settings.getAs(UcmSessionSetting.PASSWORD, EncodedString.class);

		this.context = new IdcContextSeed(userName, password);

		this.sslMode = SSLMode.decode(settings.getString(UcmSessionSetting.SSL_MODE));
		this.sslAlgorithm = Tools.toTrimmedString(settings.getString(UcmSessionSetting.SSL_ALGORITHM), true);
		if (this.sslMode != SSLMode.NONE) {

			KeyStore trustKs = null;
			String trustStore = settings.getString(UcmSessionSetting.TRUSTSTORE);
			String trustStorePassword = settings.getString(UcmSessionSetting.TRUSTSTORE_PASSWORD);
			if (trustStorePassword != null) {
				trustStorePassword = crypto.decrypt(trustStorePassword);
			}
			this.trustStore = trustStore;
			this.trustStorePassword = trustStorePassword;
			if (this.trustStore != null) {
				trustKs = KeyStoreTools.loadKeyStore(this.trustStore, this.trustStorePassword);
			}

			if (this.sslMode == SSLMode.CLIENT) {
				KeyStore clientKs = null;

				String clientStore = settings.getString(UcmSessionSetting.KEYSTORE);
				String clientStorePassword = settings.getString(UcmSessionSetting.KEYSTORE_PASSWORD);
				if (clientStorePassword != null) {
					clientStorePassword = crypto.decrypt(clientStorePassword);
				}
				this.clientStore = clientStore;
				this.clientStorePassword = clientStorePassword;

				clientKs = KeyStoreTools.loadKeyStore(this.clientStore, this.clientStorePassword);

				String clientCertAlias = settings.getString(UcmSessionSetting.CLIENT_CERT_ALIAS);
				if (StringUtils.isEmpty(clientCertAlias)) {
					throw new Exception("The client certificate alias may not be empty");
				}
				String clientCertPassword = settings.getString(UcmSessionSetting.CLIENT_CERT_PASSWORD);
				if (clientCertPassword != null) {
					clientCertPassword = crypto.decrypt(clientCertPassword);
				}
				this.clientCertAlias = clientCertAlias;
				this.clientCertPassword = clientCertPassword;

				char[] passChars = (this.clientCertPassword != null ? this.clientCertPassword.toCharArray() : null);
				try {
					Key key = clientKs.getKey(this.clientCertAlias, passChars);
					if (key == null) {
						throw new Exception(
							String.format("No private key with alias [%s] was found in the keystore at [%s]",
								this.clientCertAlias, this.clientStore));
					}
				} catch (NoSuchAlgorithmException e) {
					throw new Exception(
						String.format("The algorithm to decode the key [%s] in the keystore at [%s] could not be found",
							this.clientCertAlias, this.clientStore),
						e);
				} catch (UnrecoverableKeyException e) {
					throw new Exception(String.format(
						"Could not recover the [%s] key from the keystore at [%s] - maybe the password is incorrect?",
						this.clientCertAlias, this.clientStore), e);
				} finally {
					// Clear out the password characters...
					for (int i = 0; i < passChars.length; i++) {
						passChars[i] = '\0';
					}
				}

				if ((trustKs != null) && (clientKs != null)) {
					// TODO: If we have both a client store and a trust store, verify that the
					// client certificate is trusted as per the trust store...???
				}

			} else {
				this.clientStore = null;
				this.clientStorePassword = null;
				this.clientCertAlias = null;
				this.clientCertPassword = null;
			}

		} else {
			// No SSL_MODE-related stuff will be checked
			this.trustStore = null;
			this.trustStorePassword = null;
			this.clientStore = null;
			this.clientStorePassword = null;
			this.clientCertAlias = null;
			this.clientCertPassword = null;
		}
		// If SSL_MODE, use idcs:// insteaed of idc://
		// Always tack on the port number at the end
		this.url = String.format("idc%s://%s:%d", (this.sslMode != SSLMode.NONE) ? "s" : "", this.host, this.port);

		// TODO: Get the cache size configuration
		this.model = new UcmModel();
	}

	@Override
	public PooledObject<UcmSession> makeObject() throws Exception {

		this.log.trace("UcmSetting the IDC connection URL to [{}]...", this.url);
		IntradocClient client = IntradocClient.class.cast(this.manager.createClient(this.url));
		IntradocClientConfig config = client.getConfig();
		config.setConnectionPool("simple");
		if (this.sslMode != SSLMode.NONE) {
			if (!StringUtils.isBlank(this.sslAlgorithm)) {
				config.setAlgorithm(this.sslAlgorithm);
			}
			if (this.trustStore != null) {
				config.setTrustManagerFile(this.trustStore);
				config.setTrustManagerPassword(this.trustStorePassword);
			}
			if (this.clientStore != null) {
				config.setKeystoreFile(this.clientStore);
				config.setKeystorePassword(this.clientStorePassword);
				config.setKeystoreAlias(this.clientCertAlias);
				config.setKeystoreAliasPassword(this.clientCertPassword);
			}
		}
		if (this.socketTimeout != null) {
			config.setSocketTimeout(this.socketTimeout.intValue());
		}
		client.initialize();
		return new DefaultPooledObject<>(new UcmSession(this.model, client, this.context.newInstance()));
	}

	@Override
	public void destroyObject(PooledObject<UcmSession> p) throws Exception {
		p.getObject().logout();
	}

	@Override
	public boolean validateObject(PooledObject<UcmSession> p) {
		if (p == null) { return false; }
		UcmSession session = p.getObject();
		if (session == null) { return false; }
		if (!session.isInitialized()) { return false; }

		// If ping has been disabled, assume it's OK...
		if (this.minPingTime <= 0) { return true; }

		// If we have a ping interval, compare it
		if ((System.currentTimeMillis() - p.getLastUsedTime()) <= TimeUnit.SECONDS.toMillis(this.minPingTime)) {
			return true;
		}

		// It's time to ping the server, so do it!
		try {
			// Convert the response to a dataBinder
			DataBinder responseData = session.callService("PING_SERVER").getResponseAsBinder();
			// Display the status of the service call
			this.log.debug("Server pinged - status: {}", responseData.getLocal("StatusMessage"));
			return true;
		} catch (Exception e) {
			this.log.debug("Failed to ping the server", e);
			return false;
		}
	}

	@Override
	public void activateObject(PooledObject<UcmSession> p) throws Exception {
		// TODO: do we need to do something here?
	}

	@Override
	public void passivateObject(PooledObject<UcmSession> p) throws Exception {
		// TODO: do we need to do something here?
	}

	@Override
	protected UcmSessionWrapper newWrapper(UcmSession session) throws SessionFactoryException {
		return new UcmSessionWrapper(this, session);
	}
}