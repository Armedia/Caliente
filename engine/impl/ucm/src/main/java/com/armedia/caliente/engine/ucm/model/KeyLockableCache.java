package com.armedia.caliente.engine.ucm.model;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

import com.armedia.commons.utilities.LockDispenser;

public class KeyLockableCache<K, V> {

	private static final int MIN_LIMIT = 1000;

	private static class Locker<K> extends LockDispenser<K, ReentrantReadWriteLock> {
		@Override
		protected ReentrantReadWriteLock newLock(K key) {
			return new ReentrantReadWriteLock();
		}
	}

	private final Map<K, Reference<V>> cache;
	private final Locker<K> locks = new Locker<>();

	public KeyLockableCache() {
		this(KeyLockableCache.MIN_LIMIT);
	}

	public KeyLockableCache(int maxCount) {
		final Map<K, Reference<V>> cache = new LRUMap<>(Math.max(KeyLockableCache.MIN_LIMIT, maxCount));
		this.cache = Collections.synchronizedMap(cache);
	}

	public final Lock getExclusiveLock(K key) {
		return this.locks.getLock(key).writeLock();
	}

	public final Lock getSharedLock(K key) {
		return this.locks.getLock(key).readLock();
	}

	protected Reference<V> newReference(V value) {
		return new WeakReference<>(value);
	}

	protected final boolean threadHoldsExclusiveLock(K key) {
		return this.locks.getLock(key).isWriteLockedByCurrentThread();
	}

	public final V get(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		// This construct helps avoid deadlocks while preserving concurrency where possible
		Lock l = getSharedLock(key);
		try {
			Reference<V> ref = this.cache.get(key);
			if (ref == null) { return null; }
			return ref.get();
		} finally {
			l.unlock();
		}
	}

	public final V createIfAbsent(K key, ConcurrentInitializer<V> initializer) throws ConcurrentException {
		Objects.requireNonNull(key, "Must provide a non-null key");
		Objects.requireNonNull(initializer, "Must provide a non-null initializer");
		final Lock l = getExclusiveLock(key);
		l.lock();
		try {
			V ret = null;
			Reference<V> ref = this.cache.get(key);
			if (ref != null) {
				ret = ref.get();
			}
			// We still have a value, so return it...
			if (ret != null) { return ret; }

			// The value is absent or expired...
			V newVal = initializer.get();
			if (newVal == null) {
				this.cache.remove(key);
				throw new NullPointerException(
					String.format("The initializer created a null reference for key [%s]", key));
			}

			// Stash the new object...
			this.cache.put(key, newReference(newVal));
			return newVal;
		} finally {
			l.unlock();
		}
	}

	public final V create(K key, ConcurrentInitializer<V> initializer) throws ConcurrentException {
		Objects.requireNonNull(key, "Must provide a non-null key");
		Objects.requireNonNull(initializer, "Must provide a non-null initializer");

		final Lock l = getExclusiveLock(key);
		l.lock();
		try {
			V ret = null;
			Reference<V> ref = this.cache.get(key);
			if (ref != null) {
				ret = ref.get();
			}
			V newVal = initializer.get();
			if (newVal != null) {
				this.cache.put(key, newReference(newVal));
			} else {
				this.cache.remove(key);
			}
			return ret;
		} finally {
			l.unlock();
		}
	}

	public final V put(K key, V value) {
		Objects.requireNonNull(key, "Must provide a non-null key");

		if (value == null) { return remove(key); }
		final Lock l = getExclusiveLock(key);
		l.lock();
		try {
			V ret = null;
			Reference<V> ref = this.cache.get(key);
			if (ref != null) {
				ret = ref.get();
			}
			this.cache.put(key, newReference(value));
			return ret;
		} finally {
			l.unlock();
		}
	}

	public final V remove(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		final Lock l = getExclusiveLock(key);
		l.lock();
		try {
			Reference<V> ref = this.cache.remove(key);
			return (ref != null ? ref.get() : null);
		} finally {
			l.unlock();
		}
	}

	public final int size() {
		return this.cache.size();
	}

	public final boolean isEmpty() {
		return this.cache.isEmpty();
	}

	public final boolean containsValueForKey(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		// This construct helps avoid deadlocks while preserving concurrency where possible
		final Lock l = getSharedLock(key);
		try {
			Reference<V> ref = this.cache.get(key);
			if (ref == null) { return false; }
			return (ref.get() != null);
		} finally {
			l.unlock();
		}
	}

	public final void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
			K key = e.getKey();
			V value = e.getValue();
			if ((key != null) && (value != null)) {
				put(key, value);
			}
		}
	}

	public final void clear() {
		this.cache.clear();
	}

	public final Set<K> getKeys() {
		return new LinkedHashSet<>(this.cache.keySet());
	}

	public final Collection<V> values() {
		Collection<Reference<V>> c = new ArrayList<>(this.cache.values());
		Collection<V> ret = new ArrayList<>();
		for (Reference<V> r : c) {
			V v = r.get();
			if (v != null) {
				ret.add(v);
			}
		}
		return ret;
	}
}