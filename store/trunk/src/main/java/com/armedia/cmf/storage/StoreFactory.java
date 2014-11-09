package com.armedia.cmf.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.xml.CmsStoreConfiguration;

public abstract class StoreFactory<S> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Class<S> storeClass;

	StoreFactory(Class<S> storeClass) {
		this.storeClass = storeClass;
	}

	protected final Class<S> getStoreClass() {
		return this.storeClass;
	}

	protected abstract S newInstance(CmsStoreConfiguration cfg) throws StorageException;
}