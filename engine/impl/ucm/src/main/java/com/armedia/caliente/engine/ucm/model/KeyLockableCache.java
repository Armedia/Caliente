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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

import com.armedia.commons.utilities.LockDispenser;

public class KeyLockableCache<K, V> {

	public static final int MIN_LIMIT = 1000;
	public static final TimeUnit DEFAULT_MAX_AGE_UNIT = TimeUnit.MINUTES;
	public static final long DEFAULT_MAX_AGE = 5;

	private static class Locker<K> extends LockDispenser<K, ReentrantReadWriteLock> {
		@Override
		protected ReentrantReadWriteLock newLock(K key) {
			return new ReentrantReadWriteLock();
		}
	}

	private final class CacheItem {
		private final long creationDate;
		private final Reference<V> value;

		private CacheItem(V value) {
			this.value = newReference(value);
			this.creationDate = System.currentTimeMillis();
		}

		private V get() {
			// If the age is negative, it NEVER expires unless the GC reclaims it...
			if (KeyLockableCache.this.maxAge < 0) { return this.value.get(); }

			// If the age is 0, then it expires immediately
			if (KeyLockableCache.this.maxAge == 0) { return null; }

			// Let's check if it's expired...
			final long age = System.currentTimeMillis() - this.creationDate;
			if (age > KeyLockableCache.this.maxAgeUnit.toMillis(KeyLockableCache.this.maxAge)) { return null; }

			// It's not expired, so it's up to the GC
			return this.value.get();
		}
	}

	private final TimeUnit maxAgeUnit;
	private final long maxAge;
	private final Map<K, CacheItem> cache;
	private final Locker<K> locks = new Locker<>();

	public KeyLockableCache() {
		this(KeyLockableCache.MIN_LIMIT, KeyLockableCache.DEFAULT_MAX_AGE_UNIT, KeyLockableCache.DEFAULT_MAX_AGE);
	}

	public KeyLockableCache(int maxCount) {
		this(maxCount, KeyLockableCache.DEFAULT_MAX_AGE_UNIT, KeyLockableCache.DEFAULT_MAX_AGE);
	}

	public KeyLockableCache(TimeUnit maxAgeUnit, long maxAge) {
		this(KeyLockableCache.MIN_LIMIT, maxAgeUnit, maxAge);
	}

	public KeyLockableCache(int maxCount, TimeUnit maxAgeUnit, long maxAge) {
		final Map<K, CacheItem> cache = new LRUMap<>(Math.max(KeyLockableCache.MIN_LIMIT, maxCount));
		this.cache = Collections.synchronizedMap(cache);
		if (maxAgeUnit == null) {
			this.maxAgeUnit = KeyLockableCache.DEFAULT_MAX_AGE_UNIT;
			this.maxAge = KeyLockableCache.DEFAULT_MAX_AGE;
		} else {
			this.maxAgeUnit = maxAgeUnit;
			this.maxAge = Math.max(-1, maxAge);
		}
	}

	public final Lock getExclusiveLock(K key) {
		return this.locks.getLock(key).writeLock();
	}

	public final Lock getSharedLock(K key) {
		return this.locks.getLock(key).readLock();
	}

	/**
	 * This method is only invoked from within the CacheItem nested class
	 *
	 * @param value
	 * @return a new {@link Reference} object encapsulating the given value
	 */
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
		l.lock();
		try {
			CacheItem item = this.cache.get(key);
			if (item == null) { return null; }
			return item.get();
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
			CacheItem ref = this.cache.get(key);
			if (ref != null) {
				ret = ref.get();
			}
			// We still have a value, so return it...
			if (ret != null) { return ret; }

			// The value is absent or expired...
			V newVal = initializer.get();
			if (newVal != null) {
				this.cache.put(key, new CacheItem(newVal));
			} else {
				this.cache.remove(key);
			}

			return newVal;
		} finally {
			l.unlock();
		}
	}

	public final void putIfAbsent(K key, V value) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		if (value == null) { return; }

		V existing = get(key);
		if (existing != null) { return; }

		final Lock l = getExclusiveLock(key);
		l.lock();
		try {
			CacheItem ref = this.cache.get(key);
			if ((ref != null) && (ref.get() != null)) { return; }
			this.cache.put(key, new CacheItem(value));
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
			CacheItem ref = this.cache.get(key);
			if (ref != null) {
				ret = ref.get();
			}
			this.cache.put(key, new CacheItem(value));
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
			CacheItem ref = this.cache.remove(key);
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
		l.lock();
		try {
			CacheItem ref = this.cache.get(key);
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
		Collection<V> ret = new ArrayList<>();
		for (CacheItem r : new ArrayList<>(this.cache.values())) {
			V v = r.get();
			if (v != null) {
				ret.add(v);
			}
		}
		return ret;
	}
}