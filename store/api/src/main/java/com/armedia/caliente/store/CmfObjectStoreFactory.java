package com.armedia.caliente.store;

import java.util.Collection;

public abstract class CmfObjectStoreFactory<OPERATION extends CmfStoreOperation<?>, STORE extends CmfObjectStore<OPERATION>>
	extends CmfStoreFactory<STORE> {

	protected CmfObjectStoreFactory(String... aliases) {
		super(aliases);
	}

	protected CmfObjectStoreFactory(Collection<String> aliases) {
		super(aliases);
	}

}