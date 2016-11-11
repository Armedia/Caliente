package com.armedia.caliente.engine.importer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class SynchronizedCounter {
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Condition changed = this.rwLock.writeLock().newCondition();
	private long counter = 0;

	public long get() {
		Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.counter;
		} finally {
			l.unlock();
		}
	}

	public long add(long c) {
		Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			long ret = (this.counter += c);
			this.changed.signal();
			return ret;
		} finally {
			l.unlock();
		}
	}

	public long subtract(long c) {
		return add(-c);
	}

	public long increment() {
		return add(1);
	}

	public long decrement() {
		return subtract(1);
	}

	public void waitUntil(final long value) throws InterruptedException {
		waitUntil(value, 0, TimeUnit.SECONDS);
	}

	public void waitUntil(final long value, long timeout, TimeUnit timeUnit) throws InterruptedException {
		Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			while (value != this.counter) {
				if (timeout > 0) {
					this.changed.await(timeout, timeUnit);
				} else {
					this.changed.await();
				}
			}
			this.changed.signal();
		} finally {
			l.unlock();
		}
	}

	public long waitForChange() throws InterruptedException {
		return waitForChange(0, TimeUnit.SECONDS);
	}

	public long waitForChange(long timeout, TimeUnit timeUnit) throws InterruptedException {
		Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (timeout > 0) {
				this.changed.await(timeout, timeUnit);
			} else {
				this.changed.await();
			}
			final long ret = this.counter;
			this.changed.signal();
			return ret;
		} finally {
			l.unlock();
		}
	}

	@Override
	public String toString() {
		return String.format("SynchronizedCounter[%08x]", this.counter);
	}
}