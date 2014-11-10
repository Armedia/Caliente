package com.armedia.cmf.storage;

import java.util.Collection;

public abstract class ContentStoreFactory<S extends ContentStore> extends StoreFactory<S> {

	protected ContentStoreFactory(String... aliases) {
		super(aliases);
	}

	protected ContentStoreFactory(Collection<String> aliases) {
		super(aliases);
	}

}