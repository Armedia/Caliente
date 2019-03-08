package com.armedia.caliente.tools;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;

public class ReadWriteIterator<E> extends BaseReadWriteLockable implements Iterator<E> {
	private final Iterator<E> iterator;

	public ReadWriteIterator(Iterator<E> iterator) {
		this(null, iterator);
	}

	public ReadWriteIterator(ReadWriteLock rwLock, Iterator<E> iterator) {
		super(rwLock);
		this.iterator = Objects.requireNonNull(iterator, "Must provide a non-null Iterator to back this iterator");
	}

	@Override
	public boolean hasNext() {
		return readLocked(this.iterator::hasNext);
	}

	@Override
	public E next() {
		return readLocked(this.iterator::next);
	}

	@Override
	public void remove() {
		writeLocked(this.iterator::remove);
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action) {
		Objects.requireNonNull(action, "Must provide a non-null action to apply");
		readLocked(() -> {
			this.iterator.forEachRemaining(action);
		});
	}
}