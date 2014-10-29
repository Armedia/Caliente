package com.armedia.cmf.engine;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SessionFactory<S, W extends SessionWrapper<S>> implements PoolableObjectFactory<S> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final GenericObjectPool<S> pool;

	private final Thread cleanup;

	private AtomicBoolean open = new AtomicBoolean(true);

	protected static GenericObjectPool.Config getDefaultPoolConfig() {
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

		this.cleanup = new Thread() {
			@Override
			public void run() {
				close();
			}
		};
		Runtime.getRuntime().addShutdownHook(this.cleanup);
	}

	public final void configure() {
		this.pool.setConfig(getPoolConfig());
	}

	public final W acquireSession() throws Exception {
		return newWrapper(this.pool.borrowObject());
	}

	final void releaseSession(S session) {
		try {
			this.pool.returnObject(session);
		} catch (Exception e) {
			// TODO: log it
		}
	}

	public final void close() {
		if (this.open.compareAndSet(true, false)) {
			try {
				this.pool.close();
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught closing this a factory", e);
				} else {
					this.log.error(String.format("Exception caught closing this a factory: %s", e.getMessage()));
				}
			} finally {
				Runtime.getRuntime().removeShutdownHook(this.cleanup);
			}
		}
	}

	protected GenericObjectPool.Config getPoolConfig() {
		return SessionFactory.getDefaultPoolConfig();
	}

	protected abstract W newWrapper(S session);
}