package com.armedia.cmf.storage.local;

import java.io.File;

import com.armedia.cmf.storage.CmfOperationException;
import com.armedia.cmf.storage.CmfStoreOperation;

public class LocalStoreOperation extends CmfStoreOperation<File> {

	public LocalStoreOperation(File wrapped) {
		super(wrapped);
	}

	@Override
	protected boolean supportsTransactions() {
		return false;
	}

	@Override
	protected boolean beginTransaction() throws CmfOperationException {
		return false;
	}

	@Override
	protected void commitTransaction() throws CmfOperationException {
	}

	@Override
	protected void rollbackTransaction() throws CmfOperationException {
	}

	@Override
	protected void closeConnection() throws Exception {
	}
}