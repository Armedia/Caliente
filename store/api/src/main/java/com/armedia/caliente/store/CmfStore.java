package com.armedia.caliente.store;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CmfStore<CONNECTION, OPERATION extends CmfStoreOperation<CONNECTION>> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private boolean open = true;

	protected final void assertOpen() {
		this.lock.readLock().lock();
		try {
			if (!this.open) { throw new IllegalStateException("This stream store is not open, call init() first"); }
		} finally {
			this.lock.readLock().unlock();
		}
	}

	protected final Lock getReadLock() {
		return this.lock.readLock();
	}

	protected final Lock getWriteLock() {
		return this.lock.writeLock();
	}

	protected final boolean isOpen() {
		this.lock.readLock().lock();
		try {
			return this.open;
		} finally {
			this.lock.readLock().unlock();
		}
	}

	final boolean close() {
		return close(false);
	}

	final boolean close(boolean cleanupIfEmpty) {
		this.lock.writeLock().lock();
		try {
			if (!this.open) { return false; }
			return doClose(cleanupIfEmpty);
		} finally {
			this.open = false;
			this.lock.writeLock().unlock();
		}
	}

	protected abstract OPERATION newOperation() throws CmfStorageException;

	protected final OPERATION beginConcurrentInvocation() throws CmfStorageException {
		return beginInvocation(false);
	}

	protected final OPERATION beginExclusiveInvocation() throws CmfStorageException {
		return beginInvocation(true);
	}

	private OPERATION beginInvocation(boolean exclusive) throws CmfStorageException {
		boolean ok = true;
		final Lock lock = (exclusive ? getWriteLock() : getReadLock());
		try {
			lock.lock();
			assertOpen();
			OPERATION ret = newOperation();
			ok = true;
			return ret;
		} finally {
			if (!ok) {
				lock.unlock();
			}
		}
	}

	protected final void endConcurrentInvocation(OPERATION operation) {
		endInvocation(operation, false);
	}

	protected final void endExclusiveInvocation(OPERATION operation) {
		endInvocation(operation, true);
	}

	private void endInvocation(OPERATION operation, boolean exclusive) {
		final Lock lock = (exclusive ? getWriteLock() : getReadLock());
		try {
			operation.closeQuietly();
		} finally {
			lock.unlock();
		}
	}

	protected boolean doClose(boolean cleanupIfEmpty) {
		return true;
	}

	public final CmfValue getProperty(String property) throws CmfStorageException {
		if (property == null) { throw new IllegalArgumentException("Must provide a valid property to retrieve"); }
		return doGetProperty(property);
	}

	public final CmfValue setProperty(String property, CmfValue value) throws CmfStorageException {
		if (property == null) { throw new IllegalArgumentException("Must provide a valid property to set"); }
		if (value == null) { return doClearProperty(property); }
		return doSetProperty(property, value);
	}

	public final CmfValue clearProperty(String property) throws CmfStorageException {
		if (property == null) { throw new IllegalArgumentException("Must provide a valid property to set"); }
		return doClearProperty(property);
	}

	public final void clearProperties() throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				clearProperties(operation);
				if (tx) {
					operation.commit();
				}
				ok = true;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for clearing all properties", e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract void clearProperties(OPERATION operation) throws CmfStorageException;

	protected final CmfValue doGetProperty(String property) throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getProperty(operation, property);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for retrieving the property [%s]", property), e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract CmfValue getProperty(OPERATION operation, String property) throws CmfStorageException;

	protected final CmfValue doSetProperty(String property, CmfValue value) throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				CmfValue ret = setProperty(operation, property, value);
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for setting the property [%s] to [%s]",
								property, value.asString()),
							e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract CmfValue setProperty(OPERATION operation, String property, CmfValue value) throws CmfStorageException;

	public final Set<String> getPropertyNames() throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getPropertyNames(operation);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for getting all property names", e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract Set<String> getPropertyNames(OPERATION operation) throws CmfStorageException;

	protected final CmfValue doClearProperty(String property) throws CmfStorageException {

		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				CmfValue ret = clearProperty(operation, property);
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format("Failed to rollback the transaction for clearing the property [%s]",
							property), e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract CmfValue clearProperty(OPERATION operation, String property) throws CmfStorageException;
}