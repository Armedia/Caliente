package com.armedia.cmf.storage;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.armedia.commons.utilities.CfgTools;

public abstract class CmfTypeMapper {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private boolean closed = false;

	protected final void configure(CfgTools cfg) throws Exception {
		boolean ok = false;
		this.lock.writeLock().lock();
		try {
			doConfigure(cfg);
			ok = true;
		} finally {
			if (!ok) {
				close();
			}
			this.closed = !ok;
			this.lock.writeLock().unlock();
		}
	}

	protected void doConfigure(CfgTools cfg) throws Exception {
		// Do nothing by default
	}

	protected final boolean isOpen() {
		this.lock.readLock().lock();
		try {
			return !this.closed;
		} finally {
			this.lock.readLock().unlock();
		}
	}

	/**
	 * <p>
	 * Check the mapping engine to translate the given source type into the desired mapped type. If
	 * no mapping is available, then {@code null} will be returned.
	 * </p>
	 *
	 * @param type
	 * @return the final mapping the source data translates to, or {@code null} if no such mapping
	 *         is available
	 */
	public final String mapType(String type) {
		this.lock.readLock().lock();
		try {
			if (!isOpen()) { throw new IllegalStateException("This mapper has been closed"); }
			return getMapping(type);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	/**
	 * <p>
	 * Performs the actual type mapping. Returns {@code null} if no mapping is to be performed.
	 * </p>
	 *
	 * @param sourceType
	 * @return the actual type mapping or {@code null} if no mapping is to be performed.
	 */
	protected abstract String getMapping(String sourceType);

	public final void close() {
		this.lock.writeLock().lock();
		try {
			if (!this.closed) {
				doClose();
			}
		} finally {
			this.closed = true;
			this.lock.writeLock().unlock();
		}
	}

	protected void doClose() {
		// Do nothing
	}

	public static CmfTypeMapper getTypeMapper(String name, CfgTools config) throws Exception {
		CmfTypeMapperFactory factory = CmfTypeMapperFactory.getFactory(name);
		if (factory == null) { return null; }
		return factory.getMapperInstance(config);
	}
}