package com.armedia.caliente.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CmfStoreOperation<CONNECTION> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final CONNECTION connection;
	private boolean valid = true;
	private boolean transactionOpen = false;

	protected CmfStoreOperation(CONNECTION wrapped) {
		if (wrapped == null) { throw new IllegalArgumentException(
			"Must provide the connection that this operation is related to"); }
		this.connection = wrapped;
	}

	protected final void assertValid() {
		if (!this.valid) { throw new IllegalStateException("This session is no longer valid"); }
	}

	protected abstract boolean supportsTransactions();

	public final CONNECTION getConnection() {
		assertValid();
		return this.connection;
	}

	public final boolean begin() throws CmfOperationException {
		assertValid();
		if (this.transactionOpen) { return false; }
		boolean tx = false;
		if (supportsTransactions()) {
			tx = beginTransaction();
		}
		this.transactionOpen = tx;
		return tx;
	}

	protected abstract boolean beginTransaction() throws CmfOperationException;

	public final void commit() throws CmfOperationException {
		assertValid();
		if (!this.transactionOpen) { return; }
		if (!supportsTransactions()) { throw new UnsupportedOperationException(
			"This type of session doesn't support transactions"); }
		try {
			commitTransaction();
		} finally {
			this.transactionOpen = false;
		}
	}

	protected abstract void commitTransaction() throws CmfOperationException;

	public final void rollback() throws CmfOperationException {
		assertValid();
		if (!this.transactionOpen) { return; }
		if (!supportsTransactions()) { throw new UnsupportedOperationException(
			"This type of session doesn't support transactions"); }
		try {
			rollbackTransaction();
		} finally {
			this.transactionOpen = false;
		}
	}

	protected abstract void rollbackTransaction() throws CmfOperationException;

	public final void close() throws CmfStorageException {
		if (!this.valid) { return; }
		try {
			closeConnection();
		} catch (Exception e) {
			throw new CmfStorageException("Failed to close the connection", e);
		} finally {
			this.valid = false;
		}
	}

	protected final void closeQuietly() {
		try {
			close();
		} catch (CmfStorageException e) {
			// Ignore it...
			if (this.log.isTraceEnabled()) {
				this.log.error("Exception raised while closing an operation", e);
			}
		}
	}

	protected abstract void closeConnection() throws Exception;
}