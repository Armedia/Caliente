package com.armedia.caliente.engine.tools;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class ReadWriteIterator<E> implements Iterator<E> {
	private final ReadWriteLock rwLock;
	private final Iterator<E> iterator;

	public ReadWriteIterator(Iterator<E> iterator) {
		this(new ReentrantReadWriteLock(), iterator);
	}

	public ReadWriteIterator(ReadWriteLock rwLock, Iterator<E> iterator) {
		this.rwLock = Objects.requireNonNull(rwLock, "Must provide a non-null ReadWriteLock instance");
		this.iterator = Objects.requireNonNull(iterator, "Must provide a non-null Iterator to back this iterator");
	}

	private Lock readLock() {
		Lock ret = this.rwLock.readLock();
		ret.lock();
		return ret;
	}

	private Lock writeLock() {
		Lock ret = this.rwLock.writeLock();
		ret.lock();
		return ret;
	}

	@Override
	public boolean hasNext() {
		Lock lock = readLock();
		try {
			return this.iterator.hasNext();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E next() {
		Lock lock = readLock();
		try {
			return this.iterator.next();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void remove() {
		Lock lock = writeLock();
		try {
			this.iterator.remove();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action) {
		Objects.requireNonNull(action, "Must provide a non-null action to apply");
		Lock lock = readLock();
		try {
			this.iterator.forEachRemaining(action);
		} finally {
			lock.unlock();
		}
	}
}