package com.armedia.caliente.engine.tools;

import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteSet<ELEMENT> extends ReadWriteCollection<ELEMENT> implements Set<ELEMENT> {

	public ReadWriteSet(Set<ELEMENT> set) {
		this(new ReentrantReadWriteLock(), set);
	}

	public ReadWriteSet(ReadWriteLock rwLock, Set<ELEMENT> set) {
		super(rwLock, set);
	}

}