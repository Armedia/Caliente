package com.armedia.caliente.engine.tools;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
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

	protected abstract class CacheItem {
		private final long creationDate;

		protected CacheItem() {
			this.creationDate = System.currentTimeMillis();
		}

		public final V get() {
			// If the age is negative, it NEVER expires unless the GC reclaims it...
			if (KeyLockableCache.this.maxAge < 0) { return doGet(); }

			// If the age is 0, then it expires immediately
			if (KeyLockableCache.this.maxAge == 0) { return null; }

			// Let's check if it's expired...
			final long age = System.currentTimeMillis() - this.creationDate;
			if (age > KeyLockableCache.this.maxAgeUnit.toMillis(KeyLockableCache.this.maxAge)) { return null; }

			// It's not expired, so it's up to the GC
			return doGet();
		}

		protected abstract V doGet();
	}

	protected final class DirectCacheItem extends CacheItem {
		private final V value;

		public DirectCacheItem(V value) {
			Objects.requireNonNull(value, "Must provide a non-null value");
			this.value = value;
		}

		@Override
		protected V doGet() {
			return this.value;
		}
	}

	protected class ReferenceCacheItem extends CacheItem {
		private final Reference<V> value;

		public ReferenceCacheItem(Reference<V> value) {
			super();
			Objects.requireNonNull(value, "Must provide a non-null Reference object");
			this.value = value;
		}

		@Override
		protected V doGet() {
			return this.value.get();
		}
	}

	protected final class WeakReferenceCacheItem extends ReferenceCacheItem {
		public WeakReferenceCacheItem(V value) {
			super(new WeakReference<>(value));
		}
	}

	protected final class SoftReferenceCacheItem extends ReferenceCacheItem {
		public SoftReferenceCacheItem(V value) {
			super(new SoftReference<>(value));
		}
	}

	private final TimeUnit maxAgeUnit;
	private final long maxAge;
	private final Map<K, CacheItem> cache;
	private final LockDispenser<K, ReentrantReadWriteLock> locks = new LockDispenser<K, ReentrantReadWriteLock>() {
		@Override
		protected ReentrantReadWriteLock newLock(K key) {
			return new ReentrantReadWriteLock();
		}
	};

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

	protected CacheItem newCacheItem(V value) {
		return new SoftReferenceCacheItem(value);
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
			V value = null;
			CacheItem item = this.cache.get(key);
			if (item != null) {
				value = item.get();
			}
			// We still have a value, so return it...
			if (value != null) { return value; }

			// The value is absent or expired...
			V newValue = initializer.get();
			if (newValue != null) {
				this.cache.put(key, newCacheItem(newValue));
			} else {
				this.cache.remove(key);
			}

			return newValue;
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
			CacheItem item = this.cache.get(key);
			if ((item != null) && (item.get() != null)) { return; }
			this.cache.put(key, newCacheItem(value));
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
			CacheItem item = this.cache.get(key);
			if (item != null) {
				ret = item.get();
			}
			this.cache.put(key, newCacheItem(value));
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
			CacheItem item = this.cache.remove(key);
			return (item != null ? item.get() : null);
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
			CacheItem item = this.cache.get(key);
			if (item == null) { return false; }
			return (item.get() != null);
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
		Collection<V> values = new ArrayList<>();
		for (CacheItem r : new ArrayList<>(this.cache.values())) {
			V v = r.get();
			if (v != null) {
				values.add(v);
			}
		}
		return values;
	}
}