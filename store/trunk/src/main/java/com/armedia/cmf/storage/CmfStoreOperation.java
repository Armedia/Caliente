package com.armedia.cmf.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CmfStoreOperation<C> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final C connection;
	private boolean valid = true;
	private boolean transactionOpen = false;

	protected CmfStoreOperation(C wrapped) {
		if (wrapped == null) { throw new IllegalArgumentException(
			"Must provide the connection that this operation is related to"); }
		this.connection = wrapped;
	}

	protected final void assertValid() {
		if (!this.valid) { throw new IllegalStateException("This session is no longer valid"); }
	}

	protected abstract boolean supportsTransactions();

	public final C getConnection() {
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
		close(false);
	}

	public final void close(boolean commit) throws CmfStorageException {
		if (!this.valid) { return; }
		try {
			if (this.transactionOpen) {
				if (commit) {
					commit();
				} else {
					rollback();
				}
			}
		} finally {
			closeConnectionQuietly();
			this.valid = false;
		}
	}

	protected final void closeConnectionQuietly() {
		try {
			closeConnection();
		} catch (Throwable t) {
			// Ignore it...
			if (this.log.isTraceEnabled()) {
				this.log.error("Exception raised while closing an operation's connection", t);
			}
		}
	}

	protected abstract void closeConnection() throws Exception;
}