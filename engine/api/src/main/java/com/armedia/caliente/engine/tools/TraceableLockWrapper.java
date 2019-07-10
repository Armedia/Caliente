/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.tools;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceableLockWrapper<L extends Lock> implements Lock, Serializable {
	private static final long serialVersionUID = 1L;

	private static final AtomicLong LOCK_COUNTER = new AtomicLong(0);

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AtomicLong conditionCounter = new AtomicLong(0);
	private final Serializable lockId;
	private final L lock;

	public TraceableLockWrapper(L lock) {
		this(String.format("%016x", TraceableLockWrapper.LOCK_COUNTER.getAndIncrement()), lock);
	}

	public TraceableLockWrapper(Serializable lockId, L lock) {
		this.lockId = lockId;
		this.lock = lock;
	}

	public final L getLock() {
		return this.lock;
	}

	public final Serializable getLockId() {
		return this.lockId;
	}

	@Override
	public void lock() {
		this.log.trace("Lock[{}].lock()", this.lockId);
		boolean ok = false;
		try {
			this.lock.lock();
			ok = true;
		} finally {
			this.log.trace("Lock[{}].lock() {}", this.lockId, ok ? "completed" : "FAILED");
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		this.log.trace("Lock[{}].lockInterruptibly()", this.lockId);
		boolean ok = false;
		try {
			this.lock.lockInterruptibly();
			ok = true;
		} finally {
			this.log.trace("Lock[{}].lockInterruptibly() {}", this.lockId, ok ? "completed" : "FAILED");
		}
	}

	@Override
	public boolean tryLock() {
		this.log.trace("Lock[{}].tryLock()", this.lockId);
		boolean ok = false;
		Boolean ret = null;
		try {
			ret = this.lock.tryLock();
			ok = true;
			return ret;
		} finally {
			this.log.trace("Lock[{}].tryLock() {} (returning {})", this.lockId, ok ? "completed" : "FAILED", ret);
		}
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		this.log.trace("Lock[{}].tryLock({}, {})", this.lockId, time, unit);
		boolean ok = false;
		Boolean ret = null;
		try {
			ret = this.lock.tryLock(time, unit);
			ok = true;
			return ret;
		} finally {
			this.log.trace("Lock[{}].tryLock({}, {}) {} (returning {})", this.lockId, time, unit,
				ok ? "completed" : "FAILED", ret);
		}
	}

	@Override
	public void unlock() {
		this.log.trace("Lock[{}].unlock()", this.lockId);
		boolean ok = false;
		try {
			this.lock.unlock();
			ok = true;
		} finally {
			this.log.trace("Lock[{}].unlock() {}", this.lockId, ok ? "completed" : "FAILED");
		}
	}

	@Override
	public Condition newCondition() {
		return new TraceableCondition(this.log, this.lockId,
			String.format("%016x", this.conditionCounter.getAndIncrement()), this.lock.newCondition());
	}

}