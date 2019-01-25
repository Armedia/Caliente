package com.armedia.caliente.store.local;

import java.io.File;

import com.armedia.caliente.store.CmfOperationException;
import com.armedia.caliente.store.CmfStoreOperation;

public class LocalStoreOperation extends CmfStoreOperation<File> {

	public LocalStoreOperation(File wrapped, boolean exclusive) {
		super(wrapped, exclusive);
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