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