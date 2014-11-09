package com.armedia.cmf.storage;

public abstract class ContentStoreFactory<S extends ContentStore> extends StoreFactory<S> {

	protected ContentStoreFactory(Class<S> storeClass) {
		super(storeClass);
	}

}