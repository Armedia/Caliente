package com.armedia.cmf.storage;

import java.util.Collection;

public abstract class ObjectStoreFactory<C, O extends ObjectStoreOperation<C>, S extends ObjectStore<C, O>> extends
StoreFactory<S> {

	protected ObjectStoreFactory(String... aliases) {
		super(aliases);
	}

	protected ObjectStoreFactory(Collection<String> aliases) {
		super(aliases);
	}

}