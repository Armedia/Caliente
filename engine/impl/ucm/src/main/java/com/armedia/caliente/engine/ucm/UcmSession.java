package com.armedia.caliente.engine.ucm;

import oracle.stellent.ridc.IdcClient;
import oracle.stellent.ridc.IdcClientConfig;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataFactory;
import oracle.stellent.ridc.protocol.ServiceResponse;

@SuppressWarnings("rawtypes")
public class UcmSession {

	private final IdcClient client;
	private final IdcContext userContext;

	public UcmSession(IdcClient client, IdcContext userContext) {
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
		return this.client.sendRequest(this.userContext, dataBinder);
	}

	public void setDataFactory(DataFactory dataFactory) {
		this.client.setDataFactory(dataFactory);
	}

	public void setInitialized(boolean initialized) {
		this.client.setInitialized(initialized);
	}
}