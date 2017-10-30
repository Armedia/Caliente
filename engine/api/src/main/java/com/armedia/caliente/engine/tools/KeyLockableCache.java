package com.armedia.caliente.engine.tools;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.LockDispenser;

public class KeyLockableCache<K extends Serializable, V> {

	public static final int MIN_LIMIT = 1000;
	public static final TimeUnit DEFAULT_MAX_AGE_UNIT = TimeUnit.MINUTES;
	public static final long DEFAULT_MAX_AGE = 5;

	private static final AtomicLong CACHE_ID = new AtomicLong(0);

	public static interface Expirable {
		public void expire();
	}

	protected abstract class CacheItem {
		protected final K key;
		protected final long creationDate;

		protected CacheItem(K key) {
			this.key = key;
			this.creationDate = System.currentTimeMillis();
			if (KeyLockableCache.this.log.isTraceEnabled()) {
				KeyLockableCache.this.log.trace("Cache[{}] new {} for {} (@ {}/{})", KeyLockableCache.this.cacheId,
					getClass().getSimpleName(), key, this.creationDate, new Date(this.creationDate));
			}
		}

		public final V get() {
			// If the age is negative, it NEVER expires unless the GC reclaims it...
			if (KeyLockableCache.this.maxAge < 0) {
				V ret = doGet();
				if (ret == null) {
					if (KeyLockableCache.this.log.isTraceEnabled()) {
						KeyLockableCache.this.log.trace("Cache[{}] item for {} has expired",
							KeyLockableCache.this.cacheId, this.key);
					}
					expire();
				}
				return ret;
			}

			// If the age is 0, then it expires immediately
			if (KeyLockableCache.this.maxAge == 0) {
				if (KeyLockableCache.this.log.isTraceEnabled()) {
					KeyLockableCache.this.log.trace("Cache[{}] item for {} expired immediately",
						KeyLockableCache.this.cacheId, this.key);
				}
				expire();
				return null;
			}

			// Let's check if it's expired...
			final long age = System.currentTimeMillis() - this.creationDate;
			final long maxMillis = KeyLockableCache.this.maxAgeUnit.toMillis(KeyLockableCache.this.maxAge);
			if (age >= maxMillis) {
				if (KeyLockableCache.this.log.isTraceEnabled()) {
					KeyLockableCache.this.log.trace("Cache[{}] item for {} has died of old age ({}ms >= {}ms)",
						KeyLockableCache.this.cacheId, this.key, age, maxMillis);
				}
				expire();
				return null;
			}

			// It's not expired, so it's up to the GC
			V ret = doGet();
			if (ret == null) {
				if (KeyLockableCache.this.log.isTraceEnabled()) {
					KeyLockableCache.this.log.trace("Cache[{}] item for {} has been garbage-collected",
						KeyLockableCache.this.cacheId, this.key);
				}
				expire();
			}
			return ret;
		}

		protected abstract V doGet();

		protected abstract void expire();

		protected final void callExpire(V value) {
			if (!Expirable.class.isInstance(value)) { return; }
			if (KeyLockableCache.this.log.isTraceEnabled()) {
				KeyLockableCache.this.log.trace("Cache[{}] value for {} is Expirable, calling expire()",
					KeyLockableCache.this.cacheId, this.key);
			}
			boolean ok = false;
			try {
				Expirable.class.cast(value).expire();
				ok = true;
			} finally {
				if (KeyLockableCache.this.log.isTraceEnabled()) {
					KeyLockableCache.this.log.trace("Cache[{}] value for {} was Expirable, expire() was called {}",
						KeyLockableCache.this.cacheId, this.key, ok ? "successfully" : "with errors");
				}
			}
		}
	}

	protected final class DirectCacheItem extends CacheItem {
		private final V value;

		public DirectCacheItem(K key, V value) {
			super(key);
			Objects.requireNonNull(value, "Must provide a non-null value");
			this.value = value;
		}

		@Override
		protected V doGet() {
			return this.value;
		}

		@Override
		protected void expire() {
			callExpire(this.value);
		}
	}

	protected class ReferenceCacheItem extends CacheItem {
		private final Reference<V> value;

		public ReferenceCacheItem(K key, Reference<V> value) {
			super(key);
			Objects.requireNonNull(value, "Must provide a non-null Reference object");
			this.value = value;
		}

		@Override
		protected V doGet() {
			return this.value.get();
		}

		@Override
		protected void expire() {
			V v = this.value.get();
			try {
				callExpire(v);
			} finally {
				this.value.enqueue();
			}
		}
	}

	protected final class WeakReferenceCacheItem extends ReferenceCacheItem {
		public WeakReferenceCacheItem(K key, V value) {
			super(key, new WeakReference<>(value));
		}
	}

	protected final class SoftReferenceCacheItem extends ReferenceCacheItem {
		public SoftReferenceCacheItem(K key, V value) {
			super(key, new SoftReference<>(value));
		}
	}

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final String cacheId;
	private final TimeUnit maxAgeUnit;
	private final long maxAge;
	private final Map<K, CacheItem> cache;
	private final LockDispenser<K, ReentrantReadWriteLock> locks = new LockDispenser<K, ReentrantReadWriteLock>() {
		@Override
		protected ReentrantReadWriteLock newLock(K key) {
			return new TraceableReentrantReadWriteLock(key);
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
		this.cacheId = String.format("%016x", KeyLockableCache.CACHE_ID.getAndIncrement());
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

	protected CacheItem newCacheItem(K key, V value) {
		return new SoftReferenceCacheItem(key, value);
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
				this.cache.put(key, newCacheItem(key, newValue));
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
			this.cache.put(key, newCacheItem(key, value));
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
			this.cache.put(key, newCacheItem(key, value));
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