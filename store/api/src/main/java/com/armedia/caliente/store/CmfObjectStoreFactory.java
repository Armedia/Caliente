package com.armedia.caliente.store;

import java.util.Collection;

public abstract class CmfObjectStoreFactory<C, O extends CmfStoreOperation<C>, S extends CmfObjectStore<C, O>> extends
	CmfStoreFactory<S> {

	protected CmfObjectStoreFactory(String... aliases) {
		super(aliases);
	}

	protected CmfObjectStoreFactory(Collection<String> aliases) {
		super(aliases);
	}

}