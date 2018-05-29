package com.armedia.caliente.store;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CmfStore<C, O extends CmfStoreOperation<C>> {

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

	protected abstract O newOperation() throws CmfStorageException;

	protected final O beginConcurrentInvocation() throws CmfStorageException {
		return beginInvocation(false);
	}

	protected final O beginExclusiveInvocation() throws CmfStorageException {
		return beginInvocation(true);
	}

	private O beginInvocation(boolean exclusive) throws CmfStorageException {
		boolean ok = true;
		final Lock lock = (exclusive ? getWriteLock() : getReadLock());
		try {
			lock.lock();
			assertOpen();
			O ret = newOperation();
			ok = true;
			return ret;
		} finally {
			if (!ok) {
				lock.unlock();
			}
		}
	}

	protected final void endConcurrentInvocation(O operation) {
		endInvocation(operation, false);
	}

	protected final void endExclusiveInvocation(O operation) {
		endInvocation(operation, true);
	}

	private void endInvocation(O operation, boolean exclusive) {
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

	public final Map<String, CmfValue> setProperties(Map<String, CmfValue> properties) throws CmfStorageException {
		if (properties == null) { throw new IllegalArgumentException(
			"Must provide a valid set of properties to store"); }
		if (properties.isEmpty()) { return properties; }
		return doSetProperties(properties);
	}

	public final CmfValue clearProperty(String property) throws CmfStorageException {
		if (property == null) { throw new IllegalArgumentException("Must provide a valid property to set"); }
		return doClearProperty(property);
	}

	public final Map<String, CmfValue> clearProperties(String... properties) throws CmfStorageException {
		if (properties == null) { throw new IllegalArgumentException(
			"Must provide a valid collection of property names to clear"); }
		return doClearProperties(Arrays.asList(properties));
	}

	public final Map<String, CmfValue> clearProperties(Collection<String> properties) throws CmfStorageException {
		if (properties == null) { throw new IllegalArgumentException(
			"Must provide a valid collection of property names to clear"); }
		return doClearProperties(properties);
	}

	protected final Map<String, CmfValue> doClearProperties(Collection<String> properties) throws CmfStorageException {
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			Map<String, CmfValue> ret = new TreeMap<>();
			try {
				for (String name : properties) {
					CmfValue value = clearProperty(operation, name);
					if (value != null) {
						ret.put(name, value);
					}
				}
				if (tx) {
					operation.commit();
				}
				ok = true;
				return new LinkedHashMap<>(ret);
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for setting the properties from %s", properties), e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	public final void clearAllProperties() throws CmfStorageException {
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				clearAllProperties(operation);
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

	protected abstract void clearAllProperties(O operation) throws CmfStorageException;

	protected final CmfValue doGetProperty(String property) throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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

	protected abstract CmfValue getProperty(O operation, String property) throws CmfStorageException;

	protected final CmfValue doSetProperty(String property, CmfValue value) throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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

	protected final Map<String, CmfValue> doSetProperties(Map<String, CmfValue> properties) throws CmfStorageException {
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			Map<String, CmfValue> ret = new TreeMap<>();
			try {
				for (String name : properties.keySet()) {
					CmfValue value = properties.get(name);
					if (value == null) {
						clearProperty(operation, name);
					} else {
						ret.put(name, setProperty(operation, name, value));
					}
				}
				if (tx) {
					operation.commit();
				}
				ok = true;
				return new LinkedHashMap<>(ret);
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for setting the properties from %s", properties), e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract CmfValue setProperty(O operation, String property, CmfValue value) throws CmfStorageException;

	public final Set<String> getPropertyNames() throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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

	protected abstract Set<String> getPropertyNames(O operation) throws CmfStorageException;

	protected final CmfValue doClearProperty(String property) throws CmfStorageException {

		O operation = beginConcurrentInvocation();
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

	protected abstract CmfValue clearProperty(O operation, String property) throws CmfStorageException;
}