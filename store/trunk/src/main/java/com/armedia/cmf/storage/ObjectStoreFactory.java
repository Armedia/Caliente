package com.armedia.cmf.storage;

public abstract class ObjectStoreFactory<C, O extends ObjectStoreOperation<C>, S extends ObjectStore<C, O>> extends
	StoreFactory<S> {

	protected ObjectStoreFactory(Class<S> storeClass) {
		super(storeClass);
	}
}