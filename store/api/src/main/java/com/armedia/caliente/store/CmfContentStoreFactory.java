package com.armedia.caliente.store;

import java.util.Collection;

public abstract class CmfContentStoreFactory<STORE extends CmfContentStore<?, ?>> extends CmfStoreFactory<STORE> {

	protected CmfContentStoreFactory(String... aliases) {
		super(aliases);
	}

	protected CmfContentStoreFactory(Collection<String> aliases) {
		super(aliases);
	}

}