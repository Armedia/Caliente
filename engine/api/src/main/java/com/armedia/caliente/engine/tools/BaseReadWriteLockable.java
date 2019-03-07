package com.armedia.caliente.engine.tools;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BaseReadWriteLockable implements ReadWriteLockable {

	private final ReadWriteLock rwLock;

	public BaseReadWriteLockable() {
		this(new ReentrantReadWriteLock());
	}

	public BaseReadWriteLockable(ReadWriteLock rwLock) {
		this.rwLock = Objects.requireNonNull(rwLock, "Must provide a non-null ReadWriteLock instance");
	}

	@Override
	public ReadWriteLock get() {
		return this.rwLock;
	}
}