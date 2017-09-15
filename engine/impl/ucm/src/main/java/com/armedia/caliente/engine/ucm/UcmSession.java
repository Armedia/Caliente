package com.armedia.caliente.engine.ucm;

import org.apache.commons.pool2.TrackedUse;

import oracle.stellent.ridc.IdcClient;
import oracle.stellent.ridc.IdcClientConfig;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataFactory;
import oracle.stellent.ridc.protocol.ServiceResponse;
import oracle.stellent.ridc.protocol.intradoc.IntradocClient;

@SuppressWarnings("rawtypes")
public class UcmSession implements TrackedUse {

	private final IntradocClient client;
	private final IdcContext userContext;
	private long lastUsed = 0;

	public UcmSession(IntradocClient client, IdcContext userContext) {
		this.client = client;
		this.userContext = userContext;
	}

	public IdcClient getClient() {
		return this.client;
	}

	public IdcClientConfig getConfig() {
		return this.client.getConfig();
	}

	public IdcContext getUserContext() {
		return this.userContext;
	}

	public DataBinder createBinder() {
		return this.client.createBinder();
	}

	public IdcClientManager getClientManager() {
		return this.client.getClientManager();
	}

	public DataFactory getDataFactory() {
		return this.client.getDataFactory();
	}

	public String getVersion() {
		return this.client.getVersion();
	}

	public void initialize() throws IdcClientException {
		this.client.initialize();
	}

	public boolean isCompatible(String version) {
		return this.client.isCompatible(version);
	}

	public boolean isInitialized() {
		return this.client.isInitialized();
	}

	public void logout() throws IdcClientException {
		this.client.logout(this.userContext);
	}

	public ServiceResponse sendRequest(DataBinder dataBinder) throws IdcClientException {
		try {
			return this.client.sendRequest(this.userContext, dataBinder);
		} finally {
			this.lastUsed = System.currentTimeMillis();
		}
	}

	public void setDataFactory(DataFactory dataFactory) {
		this.client.setDataFactory(dataFactory);
	}

	public void setInitialized(boolean initialized) {
		this.client.setInitialized(initialized);
	}

	@Override
	public long getLastUsed() {
		return this.lastUsed;
	}
}