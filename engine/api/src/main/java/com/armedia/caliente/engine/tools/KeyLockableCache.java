/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.tools;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.LockDispenser;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.ShareableLockable;
import com.armedia.commons.utilities.concurrent.ShareableMap;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.CheckedFunction;

public class KeyLockableCache<K extends Serializable, V> extends BaseShareableLockable {

	public static final int MIN_LIMIT = 1000;
	public static final TimeUnit DEFAULT_MAX_AGE_UNIT = TimeUnit.MINUTES;
	public static final long DEFAULT_MAX_AGE = 5;

	private static final AtomicLong CACHE_ID = new AtomicLong(0);

	@FunctionalInterface
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
			this.value = Objects.requireNonNull(value, "Must provide a non-null value");
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
			this.value = Objects.requireNonNull(value, "Must provide a non-null Reference object");
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
	private final LockDispenser<K, ShareableLockable> locks = new LockDispenser<>((key) -> new BaseShareableLockable());

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
		this.cache = new ShareableMap<>(cache);
		if (maxAgeUnit == null) {
			this.maxAgeUnit = KeyLockableCache.DEFAULT_MAX_AGE_UNIT;
			this.maxAge = KeyLockableCache.DEFAULT_MAX_AGE;
		} else {
			this.maxAgeUnit = maxAgeUnit;
			this.maxAge = Math.max(-1, maxAge);
		}
	}

	public final ShareableLockable getLock(K key) {
		return this.locks.getLock(key);
	}

	protected CacheItem newCacheItem(K key, V value) {
		return new SoftReferenceCacheItem(key, value);
	}

	public final V get(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		// This construct helps avoid deadlocks while preserving concurrency where possible
		try (SharedAutoLock keyLock = getLock(key).autoSharedLock()) {
			try (SharedAutoLock global = autoSharedLock()) {
				CacheItem item = this.cache.get(key);
				return (item != null ? item.get() : null);
			}
		}
	}

	public final <EX extends Throwable> V createIfAbsent(K key, CheckedFunction<K, V, EX> f) throws EX {
		Objects.requireNonNull(key, "Must provide a non-null key");
		Objects.requireNonNull(f, "Must provide a non-null initializer");
		try (SharedAutoLock keyShared = getLock(key).autoSharedLock()) {
			V existing = get(key);
			if (existing == null) {
				try (MutexAutoLock keyMutex = keyShared.upgrade()) {
					existing = get(key);
					if (existing == null) {
						try (SharedAutoLock globalShared = autoSharedLock()) {
							CacheItem item = this.cache.get(key);
							if ((item != null) && (item.get() != null)) {
								existing = item.get();
							} else {
								try (MutexAutoLock globalMutex = globalShared.upgrade()) {
									this.cache.put(key, newCacheItem(key, existing = f.applyChecked(key)));
								}
							}
						}
					}
				}
			}
			return existing;
		}
	}

	public final V putIfAbsent(K key, V value) {
		return createIfAbsent(key, (k) -> value);
	}

	public final V put(K key, V value) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		if (value == null) { return remove(key); }
		try (MutexAutoLock keyMutex = getLock(key).autoMutexLock()) {
			try (MutexAutoLock globalMutex = autoMutexLock()) {
				CacheItem item = this.cache.put(key, newCacheItem(key, value));
				return (item != null ? item.get() : null);
			}
		}
	}

	public final V remove(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		try (MutexAutoLock keyMutex = getLock(key).autoMutexLock()) {
			try (SharedAutoLock globalShared = autoSharedLock()) {
				CacheItem item = null;
				if (this.cache.containsKey(key)) {
					try (MutexAutoLock globalMutex = globalShared.upgrade()) {
						if (this.cache.containsKey(key)) {
							item = this.cache.remove(key);
						}
					}
				}
				return (item != null ? item.get() : null);
			}
		}
	}

	public final int size() {
		try (SharedAutoLock global = autoSharedLock()) {
			return this.cache.size();
		}
	}

	public final boolean isEmpty() {
		try (SharedAutoLock global = autoSharedLock()) {
			return this.cache.isEmpty();
		}
	}

	public final boolean containsValueForKey(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		try (SharedAutoLock lock = getLock(key).autoSharedLock()) {
			try (SharedAutoLock global = autoSharedLock()) {
				CacheItem item = this.cache.get(key);
				if (item == null) { return false; }
				return (item.get() != null);
			}
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
		try (MutexAutoLock globalMutex = autoMutexLock()) {
			this.cache.clear();
		}
	}

	public final Set<K> getKeys() {
		try (SharedAutoLock globalShared = autoSharedLock()) {
			return new LinkedHashSet<>(this.cache.keySet());
		}
	}

	public final Collection<V> values() {
		try (SharedAutoLock globalShared = autoSharedLock()) {
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
}