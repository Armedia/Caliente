package com.armedia.caliente.engine.ucm;

import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.KeyStoreTools;
import com.armedia.commons.utilities.CfgTools;

import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.protocol.intradoc.IntradocClient;
import oracle.stellent.ridc.protocol.intradoc.IntradocClientConfig;

public class UcmSessionFactory extends SessionFactory<IdcSession> {

	public static enum SSLMode {
		//
		NONE, // No SSL support
		SERVER, // Only server validation
		CLIENT, // Both server and client validation
		//
		;

		public static SSLMode decode(String str) {
			if (str == null) { return NONE; }
			return SSLMode.valueOf(str.toUpperCase());
		}
	}

	private final IdcClientManager manager;
	private final String host;
	private final int port;
	private final SSLMode ssl;
	private final String trustStore;
	private final String trustStorePassword;
	private final String clientStore;
	private final String clientStorePassword;
	private final String clientCertAlias;
	private final String clientCertPassword;
	private final IdcContext context;

	public UcmSessionFactory(CfgTools settings, CmfCrypt crypto) throws Exception {
		super(settings, crypto);

		this.manager = new IdcClientManager();
		this.host = settings.getString(UcmSessionSetting.HOST);
		this.port = settings.getInteger(UcmSessionSetting.PORT);
		if ((this.port <= 0) || (this.port > 0xffff)) { throw new Exception(
			String.format("Port number must be a number between 1 and 65535 (got %d)", this.port)); }

		String userName = settings.getString(UcmSessionSetting.USER);
		String password = settings.getString(UcmSessionSetting.PASSWORD);

		// TODO: Support Kerberos...get a Credentials object...
		if (password != null) {
			// Try to decrypt the password if it's encrypted
			this.context = new IdcContext(userName, this.crypto.decrypt(password));
		} else {
			this.context = new IdcContext(userName);
		}

		this.ssl = SSLMode.decode(settings.getString(UcmSessionSetting.SSL_MODE));
		if (this.ssl != SSLMode.NONE) {

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

			if (this.ssl == SSLMode.CLIENT) {
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
				if (StringUtils
					.isEmpty(clientCertAlias)) { throw new Exception("The client certificate alias may not be empty"); }
				String clientCertPassword = settings.getString(UcmSessionSetting.CLIENT_CERT_PASSWORD);
				if (clientCertPassword != null) {
					clientCertPassword = crypto.decrypt(clientCertPassword);
				}
				this.clientCertAlias = clientCertAlias;
				this.clientCertPassword = clientCertPassword;

				char[] passChars = (this.clientCertPassword != null ? this.clientCertPassword.toCharArray() : null);
				try {
					Key key = clientKs.getKey(this.clientCertAlias, passChars);
					if (key == null) { throw new Exception(
						String.format("No private key with alias [%s] was found in the keystore at [%s]",
							this.clientCertAlias, this.clientStore)); }
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

	}

	@Override
	public PooledObject<IdcSession> makeObject() throws Exception {
		// If SSL_MODE, use idcs:// insteaed of idc://
		// Always tack on the port number at the end

		final String url = String.format("idc%s://%s:%d", (this.ssl != SSLMode.NONE) ? "s" : "", this.host, this.port);
		this.log.trace("Setting the IDC connection URL to [{}]...", url);
		IntradocClient client = IntradocClient.class.cast(this.manager.createClient(url));

		IntradocClientConfig config = client.getConfig();
		config.setConnectionPool("simple");
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
		return new DefaultPooledObject<>(new IdcSession(client, this.context));
	}

	@Override
	public void destroyObject(PooledObject<IdcSession> p) throws Exception {
		p.getObject().logout();
	}

	@Override
	public boolean validateObject(PooledObject<IdcSession> p) {
		// TODO: Check the idle state against max idle...
		return (p != null) && p.getObject().isInitialized();
	}

	@Override
	public void activateObject(PooledObject<IdcSession> p) throws Exception {
		// TODO: do we need to do something here?
	}

	@Override
	public void passivateObject(PooledObject<IdcSession> p) throws Exception {
		// TODO: do we need to do something here?
	}

	@Override
	protected UcmSessionWrapper newWrapper(IdcSession session) throws Exception {
		return new UcmSessionWrapper(this, session);
	}
}