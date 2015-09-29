package com.armedia.cmf.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ObjectStoreOperation<C> {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private final C connection;
	private boolean valid = true;
	private boolean transactionOpen = false;

	protected ObjectStoreOperation(C wrapped) {
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

	public final boolean begin() throws StorageException {
		assertValid();
		if (this.transactionOpen) { return false; }
		boolean tx = false;
		if (supportsTransactions()) {
			try {
				tx = beginTransaction();
			} catch (Exception e) {
				throw new StorageException("Failed to begin a new transaction", e);
			}
		}
		this.transactionOpen = tx;
		return tx;
	}

	protected abstract boolean beginTransaction() throws Exception;

	public final void commit() {
		assertValid();
		if (!this.transactionOpen) { return; }
		if (!supportsTransactions()) { throw new UnsupportedOperationException(
			"This type of session doesn't support transactions"); }
		try {
			commitTransaction();
		} catch (Exception e) {
			this.log.warn("Exception raised committing an operation", e);
		} finally {
			this.transactionOpen = false;
		}
	}

	protected abstract void commitTransaction() throws Exception;

	public final void rollback() {
		assertValid();
		if (!this.transactionOpen) { return; }
		if (!supportsTransactions()) { throw new UnsupportedOperationException(
			"This type of session doesn't support transactions"); }
		try {
			rollbackTransaction();
		} catch (Exception e) {
			this.log.warn("Exception raised rolling back an operation", e);
		} finally {
			this.transactionOpen = false;
		}
	}

	protected abstract void rollbackTransaction() throws Exception;

	public final void close() throws StorageException {
		if (!this.valid) { return; }
		try {
			closeConnection();
		} catch (Exception e) {
			if (this.log.isTraceEnabled()) {
				this.log.error("Exception raised while closing an operation's connection", e);
			}
		} finally {
			this.valid = false;
		}
	}

	protected final void closeConnectionQuietly() {
		try {
			closeConnection();
		} catch (Exception e) {
			// Ignore it...
			if (this.log.isTraceEnabled()) {
				this.log.error("Exception caught while closing a connection", e);
			}
		}
	}

	protected abstract void closeConnection() throws Exception;
}