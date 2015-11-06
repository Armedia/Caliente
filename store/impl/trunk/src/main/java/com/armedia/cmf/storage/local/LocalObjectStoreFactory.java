package com.armedia.cmf.storage.local;

import java.io.File;

import com.armedia.cmf.storage.CmfObjectStoreFactory;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.xml.StoreConfiguration;

public class LocalObjectStoreFactory extends CmfObjectStoreFactory<File, LocalStoreOperation, LocalObjectStore> {

	public LocalObjectStoreFactory() {
		super("local", "fs");
	}

	@Override
	protected LocalObjectStore newInstance(StoreConfiguration cfg, boolean cleanData) throws CmfStorageException {
		return new LocalObjectStore();
	}
}