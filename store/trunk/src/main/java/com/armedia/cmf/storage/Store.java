package com.armedia.cmf.storage;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Store {

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
		this.lock.writeLock().lock();
		try {
			if (!this.open) { return false; }
			return doClose();
		} finally {
			this.open = false;
			this.lock.writeLock().unlock();
		}
	}

	protected boolean doClose() {
		return true;
	}
}