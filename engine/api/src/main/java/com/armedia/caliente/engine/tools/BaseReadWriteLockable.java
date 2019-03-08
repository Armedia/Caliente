package com.armedia.caliente.engine.tools;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BaseReadWriteLockable implements ReadWriteLockable {

	protected final ReadWriteLock rwLock;

	public BaseReadWriteLockable() {
		this(null);
	}

	public BaseReadWriteLockable(ReadWriteLock rwLock) {
		this.rwLock = (rwLock != null ? rwLock : new ReentrantReadWriteLock());
	}

	@Override
	public final ReadWriteLock get() {
		return this.rwLock;
	}
}