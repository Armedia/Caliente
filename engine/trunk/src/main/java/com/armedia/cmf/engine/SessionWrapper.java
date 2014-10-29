package com.armedia.cmf.engine;

public abstract class SessionWrapper<S> {

	private final SessionFactory<S, ?> factory;
	private final S wrapped;
	private boolean valid = true;
	private boolean transactionOpen = false;

	protected SessionWrapper(SessionFactory<S, ?> factory, S wrapped) {
		this.factory = factory;
		this.wrapped = wrapped;
	}

	protected final void assertValid() {
		if (!this.valid) { throw new IllegalStateException("This session is no longer valid"); }
	}

	public abstract String getId();

	protected abstract boolean supportsTransactions();

	public final S getWrapped() {
		assertValid();
		return this.wrapped;
	}

	public final boolean begin() {
		assertValid();
		boolean tx = false;
		if (supportsTransactions()) {
			tx = beginTransaction();
		}
		this.transactionOpen = tx;
		return tx;
	}

	protected abstract boolean beginTransaction();

	public final void commit() {
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

	protected abstract void commitTransaction();

	public final void rollback() {
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

	protected abstract void rollbackTransaction();

	public final void close() {
		if (!this.valid) { return; }
		rollback();
		try {
			this.factory.releaseSession(this.wrapped);
		} finally {
			this.valid = false;
		}
	}
}