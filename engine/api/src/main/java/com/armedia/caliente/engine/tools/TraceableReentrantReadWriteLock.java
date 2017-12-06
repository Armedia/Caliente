package com.armedia.caliente.engine.tools;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceableReentrantReadWriteLock extends ReentrantReadWriteLock {
	private static final long serialVersionUID = 1L;

	private static final AtomicLong LOCK_COUNTER = new AtomicLong(0);

	private class TraceableReadLock extends ReadLock {
		private static final long serialVersionUID = 1L;

		private TraceableReadLock(ReentrantReadWriteLock lock) {
			super(lock);
		}

		@Override
		public void lock() {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.lock()", TraceableReentrantReadWriteLock.this.lockId);
			boolean ok = false;
			try {
				super.lock();
				ok = true;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.lock() {}", TraceableReentrantReadWriteLock.this.lockId,
					ok ? "completed" : "FAILED");
			}
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.lockInterruptibly()",
				TraceableReentrantReadWriteLock.this.lockId);
			boolean ok = false;
			try {
				super.lockInterruptibly();
				ok = true;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.lockInterruptibly() {}",
					TraceableReentrantReadWriteLock.this.lockId, ok ? "completed" : "FAILED");
			}
		}

		@Override
		public boolean tryLock() {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.tryLock()", TraceableReentrantReadWriteLock.this.lockId);
			boolean ok = false;
			Boolean ret = null;
			try {
				ret = super.tryLock();
				ok = true;
				return ret;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.tryLock() {} (returning {})",
					TraceableReentrantReadWriteLock.this.lockId, ok ? "completed" : "FAILED", ret);
			}
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.tryLock({}, {})",
				TraceableReentrantReadWriteLock.this.lockId, time, unit);
			boolean ok = false;
			Boolean ret = null;
			try {
				ret = super.tryLock(time, unit);
				ok = true;
				return ret;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.tryLock({}, {}) {} (returning {})",
					TraceableReentrantReadWriteLock.this.lockId, time, unit, ok ? "completed" : "FAILED", ret);
			}
		}

		@Override
		public void unlock() {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.unlock()", TraceableReentrantReadWriteLock.this.lockId);
			boolean ok = false;
			try {
				super.unlock();
				ok = true;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].ReadLock.unlock() {}",
					TraceableReentrantReadWriteLock.this.lockId, ok ? "completed" : "FAILED");
			}
		}

		@Override
		public Condition newCondition() {
			Condition condition = super.newCondition();
			final String conditionId = String.format("%016x",
				TraceableReentrantReadWriteLock.this.conditionCounter.getAndIncrement());
			return new TraceableCondition(TraceableReentrantReadWriteLock.this.log, TraceableReentrantReadWriteLock.this.lockId,
				conditionId, condition);
		}
	}

	private class TraceableWriteLock extends WriteLock {
		private static final long serialVersionUID = 1L;

		private TraceableWriteLock(ReentrantReadWriteLock lock) {
			super(lock);
		}

		@Override
		public void lock() {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.lock()", TraceableReentrantReadWriteLock.this.lockId);
			boolean ok = false;
			try {
				super.lock();
				ok = true;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.lock() {}",
					TraceableReentrantReadWriteLock.this.lockId, ok ? "completed" : "FAILED");
			}
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.lockInterruptibly()",
				TraceableReentrantReadWriteLock.this.lockId);
			boolean ok = false;
			try {
				super.lockInterruptibly();
				ok = true;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.lockInterruptibly() {}",
					TraceableReentrantReadWriteLock.this.lockId, ok ? "completed" : "FAILED");
			}
		}

		@Override
		public boolean tryLock() {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.tryLock()", TraceableReentrantReadWriteLock.this.lockId);
			boolean ok = false;
			Boolean ret = null;
			try {
				ret = super.tryLock();
				ok = true;
				return ret;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.tryLock() {} (returning {})",
					TraceableReentrantReadWriteLock.this.lockId, ok ? "completed" : "FAILED", ret);
			}
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.tryLock({}, {})",
				TraceableReentrantReadWriteLock.this.lockId, time, unit);
			boolean ok = false;
			Boolean ret = null;
			try {
				ret = super.tryLock(time, unit);
				ok = true;
				return ret;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.tryLock({}, {}) {} (returning {})",
					TraceableReentrantReadWriteLock.this.lockId, time, unit, ok ? "completed" : "FAILED", ret);
			}
		}

		@Override
		public void unlock() {
			TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.unlock()", TraceableReentrantReadWriteLock.this.lockId);
			boolean ok = false;
			try {
				super.unlock();
				ok = true;
			} finally {
				TraceableReentrantReadWriteLock.this.log.trace("Lock[{}].WriteLock.unlock() {}",
					TraceableReentrantReadWriteLock.this.lockId, ok ? "completed" : "FAILED");
			}
		}

		@Override
		public Condition newCondition() {
			Condition condition = super.newCondition();
			final String conditionId = String.format("%016x",
				TraceableReentrantReadWriteLock.this.conditionCounter.getAndIncrement());
			return new TraceableCondition(TraceableReentrantReadWriteLock.this.log, TraceableReentrantReadWriteLock.this.lockId,
				conditionId, condition);
		}
	}

	private final AtomicLong conditionCounter = new AtomicLong(0);
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Serializable lockId;
	private final TraceableReadLock readLock;
	private final TraceableWriteLock writeLock;

	public TraceableReentrantReadWriteLock() {
		this(false);
	}

	public TraceableReentrantReadWriteLock(boolean fair) {
		this(String.format("%016x", TraceableReentrantReadWriteLock.LOCK_COUNTER.getAndIncrement()), fair);
	}

	public TraceableReentrantReadWriteLock(Serializable lockId) {
		this(lockId, false);
	}

	public TraceableReentrantReadWriteLock(Serializable lockId, boolean fair) {
		super(fair);
		this.lockId = lockId;
		this.readLock = new TraceableReadLock(this);
		this.writeLock = new TraceableWriteLock(this);
	}

	public final Serializable getLockId() {
		return this.lockId;
	}

	@Override
	public ReadLock readLock() {
		return this.readLock;
	}

	@Override
	public WriteLock writeLock() {
		return this.writeLock;
	}
}