package com.armedia.caliente.engine.tools;

import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class ReadWriteSpliterator<E> implements Spliterator<E>, ReadWriteLockable {

	private final ReadWriteLock rwLock;
	private final Spliterator<E> spliterator;

	public ReadWriteSpliterator(Spliterator<E> spliterator) {
		this(new ReentrantReadWriteLock(), spliterator);
	}

	public ReadWriteSpliterator(ReadWriteLock rwLock, Spliterator<E> spliterator) {
		this.rwLock = Objects.requireNonNull(rwLock, "Must provide a non-null ReadWriteLock instannce");
		this.spliterator = Objects.requireNonNull(spliterator, "Must provide a non-null backing spliterator");
	}

	@Override
	public final ReadWriteLock get() {
		return this.rwLock;
	}

	@Override
	public boolean tryAdvance(Consumer<? super E> action) {
		return readLocked(() -> {
			return this.spliterator.tryAdvance(action);
		});
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action) {
		readLocked(() -> {
			this.spliterator.forEachRemaining(action);
		});
	}

	@Override
	public Spliterator<E> trySplit() {
		return readLocked(() -> {
			return new ReadWriteSpliterator<>(this.rwLock, this.spliterator.trySplit());
		});
	}

	@Override
	public long estimateSize() {
		return readLocked(this.spliterator::estimateSize);
	}

	@Override
	public long getExactSizeIfKnown() {
		return readLocked(this.spliterator::getExactSizeIfKnown);
	}

	@Override
	public int characteristics() {
		return this.spliterator.characteristics();
	}

	@Override
	public boolean hasCharacteristics(int characteristics) {
		return this.spliterator.hasCharacteristics(characteristics);
	}

	@Override
	public Comparator<? super E> getComparator() {
		return this.spliterator.getComparator();
	}
}