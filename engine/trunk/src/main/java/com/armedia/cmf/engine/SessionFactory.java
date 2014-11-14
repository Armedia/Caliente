package com.armedia.cmf.engine;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;

public abstract class SessionFactory<S> implements PoolableObjectFactory<S> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final GenericObjectPool<S> pool;

	private boolean open = false;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	protected static GenericObjectPool.Config getDefaultPoolConfig(CfgTools settings) {
		GenericObjectPool.Config config = new GenericObjectPool.Config();
		config.lifo = true;
		config.maxActive = -1;
		config.maxWait = 0; // wait indefinitely
		config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
		config.minIdle = 1;
		// Make sure garbage gets collected quickly
		config.testOnBorrow = true;
		config.testOnReturn = true;
		config.maxIdle = Math.max(config.minIdle, config.maxActive / 2);
		config.timeBetweenEvictionRunsMillis = 30000;
		config.minEvictableIdleTimeMillis = 45000;
		return config;
	}

	protected SessionFactory() {
		this.pool = new GenericObjectPool<S>(this);
	}

	public final void init(CfgTools settings) throws Exception {
		if (settings == null) { throw new IllegalArgumentException("Must provide the settings to configure with"); }
		this.lock.writeLock().lock();
		boolean ok = false;
		try {
			if (this.open) {
				ok = true;
				throw new Exception("This pool is already open");
			}
			doInit(settings);
			this.pool.setConfig(getPoolConfig(settings));
			ok = true;
		} finally {
			this.open = ok;
			this.lock.writeLock().unlock();
		}
	}

	protected void doInit(CfgTools settings) throws Exception {
	}

	public final SessionWrapper<S> acquireSession() throws Exception {
		this.lock.readLock().lock();
		if (!this.open) { throw new IllegalStateException("This session factory is not open"); }
		try {
			return newWrapper(this.pool.borrowObject());
		} finally {
			this.lock.readLock().unlock();
		}
	}

	final void releaseSession(S session) {
		// We specifically don't check for openness b/c we don't care...
		try {
			this.pool.returnObject(session);
		} catch (Exception e) {
			// TODO: log it...
		}
	}

	protected void doClose() {
	}

	public final void close() {
		this.lock.writeLock().lock();
		try {
			if (!this.open) { return; }
			this.pool.close();
			doClose();
		} catch (Exception e) {
			if (this.log.isDebugEnabled()) {
				this.log.error("Exception caught closing this a factory", e);
			} else {
				this.log.error(String.format("Exception caught closing this a factory: %s", e.getMessage()));
			}
		} finally {
			this.open = false;
			this.lock.writeLock().unlock();
		}
	}

	protected GenericObjectPool.Config getPoolConfig(CfgTools settings) {
		return SessionFactory.getDefaultPoolConfig(settings);
	}

	protected abstract SessionWrapper<S> newWrapper(S session) throws Exception;
}