/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.common;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

public abstract class SessionFactory<SESSION> extends BaseShareableLockable
	implements PooledObjectFactory<SESSION>, AutoCloseable {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final GenericObjectPool<SESSION> pool;

	private boolean open = true;

	protected final CmfCrypt crypto;

	protected static <SESSION> GenericObjectPoolConfig<SESSION> getDefaultPoolConfig(CfgTools settings) {
		GenericObjectPoolConfig<SESSION> config = new GenericObjectPoolConfig<>();
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

	public final SessionWrapper<SESSION> acquireSession() throws SessionFactoryException {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (!this.open) { throw new IllegalStateException("This session factory is not open"); }
			try {
				return newWrapper(this.pool.borrowObject());
			} catch (Exception e) {
				throw new SessionFactoryException("Failed to borrow an object from the pool", e);
			}
		}
	}

	final void releaseSession(SESSION session) {
		// We specifically don't check for openness b/c we don't care...
		try {
			this.pool.returnObject(session);
		} catch (Exception e) {
			if (this.log.isDebugEnabled()) {
				this.log.warn("Failed to return a released session into the pool", e);
			}
		}
	}

	protected void doClose() {
	}

	@Override
	public final void close() {
		shareLockedUpgradable(() -> this.open, () -> {
			try {
				this.pool.close();
				doClose();
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught closing this factory", e);
				} else {
					this.log.error("Exception caught closing this factory: {}", e.getMessage());
				}
			} finally {
				this.open = false;
			}
		});
	}

	protected GenericObjectPoolConfig<SESSION> getPoolConfig(CfgTools settings) {
		return SessionFactory.getDefaultPoolConfig(settings);
	}

	protected abstract SessionWrapper<SESSION> newWrapper(SESSION session) throws SessionFactoryException;
}