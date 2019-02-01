package com.armedia.caliente.store;

import java.io.File;
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

	protected abstract OPERATION newOperation(boolean exclusive) throws CmfStorageException;

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
			OPERATION ret = newOperation(exclusive);
			ok = true;
			return ret;
		} finally {
			if (!ok) {
				lock.unlock();
			}
		}
	}

	protected final void endInvocation(OPERATION operation) {
		endInvocation(operation, false);
	}

	private void endInvocation(OPERATION operation, boolean exclusive) {
		final Lock lock = (operation.isExclusive() ? getWriteLock() : getReadLock());
		try {
			operation.closeQuietly();
		} finally {
			lock.unlock();
		}
	}

	protected boolean doClose(boolean cleanupIfEmpty) {
		return true;
	}

	/**
	 * <p>
	 * Returns the {@link File} which can be used to locate the all of the store's files, if
	 * applicable for this store. If this store does not support this functionality, {@code null}
	 * should be returned.
	 * </p>
	 *
	 * @return the {@link File} which can be used to locate the content files, if applicable
	 */
	public File getStoreLocation() {
		return null;
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
		if (properties == null) {
			throw new IllegalArgumentException("Must provide a valid set of properties to store");
		}
		if (properties.isEmpty()) { return properties; }
		return doSetProperties(properties);
	}

	public final CmfValue clearProperty(String property) throws CmfStorageException {
		if (property == null) { throw new IllegalArgumentException("Must provide a valid property to set"); }
		return doClearProperty(property);
	}

	public final Map<String, CmfValue> clearProperties(String... properties) throws CmfStorageException {
		if (properties == null) {
			throw new IllegalArgumentException("Must provide a valid collection of property names to clear");
		}
		return doClearProperties(Arrays.asList(properties));
	}

	public final Map<String, CmfValue> clearProperties(Collection<String> properties) throws CmfStorageException {
		if (properties == null) {
			throw new IllegalArgumentException("Must provide a valid collection of property names to clear");
		}
		return doClearProperties(properties);
	}

	protected final Map<String, CmfValue> doClearProperties(Collection<String> properties) throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
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
						this.log.warn("Failed to rollback the transaction for setting the properties from {}",
							properties, e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	public final void clearAllProperties() throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
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
			endInvocation(operation);
		}
	}

	protected abstract void clearAllProperties(OPERATION operation) throws CmfStorageException;

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
						this.log.warn("Failed to rollback the transaction for retrieving the property [{}]", property,
							e);
					}
				}
			}
		} finally {
			endInvocation(operation);
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
						this.log.warn("Failed to rollback the transaction for setting the property [{}] to [[}]",
							property, value.asString(), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected final Map<String, CmfValue> doSetProperties(Map<String, CmfValue> properties) throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
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
						this.log.warn("Failed to rollback the transaction for setting the properties from {}",
							properties, e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract CmfValue setProperty(OPERATION operation, String property, CmfValue value)
		throws CmfStorageException;

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
			endInvocation(operation);
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
						this.log.warn("Failed to rollback the transaction for clearing the property [{}]", property, e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract CmfValue clearProperty(OPERATION operation, String property) throws CmfStorageException;
}