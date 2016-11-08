package com.armedia.caliente.store;

import java.util.Collection;

public abstract class CmfContentStoreFactory<S extends CmfContentStore<?, ?, ?>> extends CmfStoreFactory<S> {

	protected CmfContentStoreFactory(String... aliases) {
		super(aliases);
	}

	protected CmfContentStoreFactory(Collection<String> aliases) {
		super(aliases);
	}

}