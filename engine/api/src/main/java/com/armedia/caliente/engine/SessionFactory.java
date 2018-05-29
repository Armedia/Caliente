package com.armedia.caliente.engine;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public abstract class SessionFactory<SESSION> implements PooledObjectFactory<SESSION> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final GenericObjectPool<SESSION> pool;

	private boolean open = true;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	protected final CmfCrypt crypto;

	protected static GenericObjectPoolConfig getDefaultPoolConfig(CfgTools settings) {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setLifo(true);
		config.setMaxTotal(-1);
		config.setMaxWaitMillis(0); // wait indefinitely
		config.setBlockWhenExhausted(false);
		config.setMinIdle(1); // Make sure garbage gets collected quickly
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		config.setMaxIdle(Math.max(config.getMinIdle(), config.getMaxTotal() / 2));
		config.setTimeBetweenEvictionRunsMillis(30000);
		config.setMinEvictableIdleTimeMillis(45000);
		return config;
	}

	protected SessionFactory(CfgTools settings, CmfCrypt crypto) {
		this.crypto = crypto;
		this.pool = new GenericObjectPool<>(this);
		this.pool.setConfig(getPoolConfig(settings));
	}

	public final SessionWrapper<SESSION> acquireSession() throws Exception {
		this.lock.readLock().lock();
		if (!this.open) { throw new IllegalStateException("This session factory is not open"); }
		try {
			return newWrapper(this.pool.borrowObject());
		} finally {
			this.lock.readLock().unlock();
		}
	}

	final void releaseSession(SESSION session) {
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

	protected GenericObjectPoolConfig getPoolConfig(CfgTools settings) {
		return SessionFactory.getDefaultPoolConfig(settings);
	}

	protected abstract SessionWrapper<SESSION> newWrapper(SESSION session) throws Exception;
}