package com.armedia.caliente.store;

import java.util.Collection;

public abstract class CmfObjectStoreFactory<CONNECTION, OPERATION extends CmfStoreOperation<CONNECTION>, STORE extends CmfObjectStore<CONNECTION, OPERATION>>
	extends CmfStoreFactory<STORE> {

	protected CmfObjectStoreFactory(String... aliases) {
		super(aliases);
	}

	protected CmfObjectStoreFactory(Collection<String> aliases) {
		super(aliases);
	}

}