package com.armedia.caliente.store;

import java.net.URI;

import com.armedia.caliente.store.xml.StoreConfiguration;

public interface CmfStorePrep {

	public void prepareStore(StoreConfiguration cfg, boolean cleanData) throws CmfStoragePreparationException;

	public URI getStoreURI();

	public void close();
}