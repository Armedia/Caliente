package com.armedia.caliente.engine.tools;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface ReadWriteLockable extends Supplier<ReadWriteLock> {

	public default Lock readLock() {
		Lock ret = get().readLock();
		ret.lock();
		return ret;
	}

	public default Lock writeLock() {
		Lock ret = get().writeLock();
		ret.lock();
		return ret;
	}

	public default <E> E readLocked(Supplier<E> operation) {
		Objects.requireNonNull(operation, "Must provide a non-null supplier to invoke");
		final Lock l = readLock();
		try {
			return operation.get();
		} finally {
			l.unlock();
		}
	}

	public default void readLocked(Runnable operation) {
		Objects.requireNonNull(operation, "Must provide a non-null runnable to invoke");
		final Lock l = readLock();
		try {
			operation.run();
		} finally {
			l.unlock();
		}
	}

	public default <E> E writeLocked(Supplier<E> operation) {
		Objects.requireNonNull(operation, "Must provide a non-null supplier to invoke");
		final Lock l = writeLock();
		try {
			return operation.get();
		} finally {
			l.unlock();
		}
	}

	public default void writeLocked(Runnable operation) {
		Objects.requireNonNull(operation, "Must provide a non-null runnable to invoke");
		final Lock l = writeLock();
		try {
			operation.run();
		} finally {
			l.unlock();
		}
	}

	public default <E> E readUpgradable(Supplier<E> checker, Predicate<E> decision, Function<E, E> writeBlock) {
		Objects.requireNonNull(checker, "Must provide a non-null checker");
		Objects.requireNonNull(decision, "Must provide a non-null decision");
		Objects.requireNonNull(writeBlock, "Must provide a non-null writeBlock");

		final Lock readLock = readLock();
		try {
			E e = checker.get();
			if (decision.test(e)) { return e; }

			readLock.unlock();
			final Lock writeLock = writeLock();
			try {
				e = checker.get();
				if (!decision.test(e)) {
					e = writeBlock.apply(e);
				}
				readLock.lock();
				return e;
			} finally {
				writeLock.unlock();
			}
		} finally {
			readLock.unlock();
		}
	}

	public default <E> void readUpgradable(Supplier<E> checker, Predicate<E> decision, Consumer<E> writeBlock) {
		Objects.requireNonNull(checker, "Must provide a non-null checker");
		Objects.requireNonNull(decision, "Must provide a non-null decision");
		Objects.requireNonNull(writeBlock, "Must provide a non-null writeBlock");

		final Lock readLock = readLock();
		try {
			E e = checker.get();
			if (decision.test(e)) { return; }

			readLock.unlock();
			final Lock writeLock = writeLock();
			try {
				e = checker.get();
				if (!decision.test(e)) {
					writeBlock.accept(e);
				}
				readLock.lock();
			} finally {
				writeLock.unlock();
			}
		} finally {
			readLock.unlock();
		}
	}
}