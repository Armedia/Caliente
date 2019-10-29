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
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.LockDispenser;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.ShareableLockable;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.CheckedFunction;

public final class KeyLockableCache<K extends Serializable, V> extends BaseShareableLockable {

	public static final int MIN_LIMIT = 1000;
	public static final Duration DEFAULT_MAX_AGE = Duration.ofMinutes(5);
	public static final ReferenceType DEFAULT_TYPE = ReferenceType.WEAK;

	private static final AtomicLong CACHE_ID = new AtomicLong(0);

	public enum ReferenceType {
		//
		FINAL, //
		SOFT, //
		WEAK, //
		//
		;

	}

	@FunctionalInterface
	public static interface Expirable {
		public void expire();
	}

	private class CacheItem {
		protected final K key;
		protected final Instant creationDate;
		protected final Supplier<V> supplier;

		private CacheItem(K key, Supplier<V> supplier) {
			this.key = key;
			this.creationDate = Instant.now();
			this.supplier = supplier;
			if (KeyLockableCache.this.log.isTraceEnabled()) {
				KeyLockableCache.this.log.trace("Cache[{}] new {} for {} (@ {})", KeyLockableCache.this.cacheId,
					getClass().getSimpleName(), key, this.creationDate);
			}
		}

		public final V get() {
			// If the age is negative, it NEVER expires unless the GC reclaims it...
			if (KeyLockableCache.this.maxAge.isNegative()) {
				V ret = this.supplier.get();
				if (ret == null) {
					if (KeyLockableCache.this.log.isTraceEnabled()) {
						KeyLockableCache.this.log.trace("Cache[{}] item for {} has expired",
							KeyLockableCache.this.cacheId, this.key);
					}
				}
				return ret;
			}

			// If the age is 0, then it expires immediately
			if (KeyLockableCache.this.maxAge.isZero()) {
				if (KeyLockableCache.this.log.isTraceEnabled()) {
					KeyLockableCache.this.log.trace("Cache[{}] item for {} expired immediately",
						KeyLockableCache.this.cacheId, this.key);
				}
				expire();
				return null;
			}

			// Let's check if it's expired...
			final Instant now = Instant.now();
			final Duration currentAge = Duration.between(this.creationDate, now);
			if (KeyLockableCache.this.maxAge.compareTo(currentAge) <= 0) {
				if (KeyLockableCache.this.log.isTraceEnabled()) {
					KeyLockableCache.this.log.trace("Cache[{}] item for {} has died of old age ({})",
						KeyLockableCache.this.cacheId, this.key, currentAge);
				}
				expire();
				return null;
			}

			// It's not expired, so it's up to the GC
			V ret = this.supplier.get();
			if (ret == null) {
				if (KeyLockableCache.this.log.isTraceEnabled()) {
					KeyLockableCache.this.log.trace("Cache[{}] item for {} has been garbage-collected",
						KeyLockableCache.this.cacheId, this.key);
				}
			}
			return ret;
		}

		private void expire() {
			expire(this.supplier.get());
		}

		private void expire(V value) {
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

	public final class DirectCacheItem extends CacheItem {
		public DirectCacheItem(K key, V value) {
			super(key, () -> value);
		}
	}

	private class ReferenceCacheItem extends CacheItem {
		public ReferenceCacheItem(K key, Reference<V> value) {
			super(key, value::get);
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
	private final BiFunction<K, V, ? extends CacheItem> itemConstructor;
	private final Duration maxAge;
	private final Map<K, CacheItem> cache;
	private final LockDispenser<K, ShareableLockable> locks = new LockDispenser<>((key) -> new BaseShareableLockable());

	public KeyLockableCache() {
		this(null, KeyLockableCache.MIN_LIMIT, KeyLockableCache.DEFAULT_MAX_AGE);
	}

	public KeyLockableCache(Duration maxAge) {
		this(null, KeyLockableCache.MIN_LIMIT, maxAge);
	}

	public KeyLockableCache(int maxCount) {
		this(null, maxCount, KeyLockableCache.DEFAULT_MAX_AGE);
	}

	public KeyLockableCache(int maxCount, Duration maxAge) {
		this(null, maxCount, maxAge);
	}

	public KeyLockableCache(ReferenceType type) {
		this(type, KeyLockableCache.MIN_LIMIT, KeyLockableCache.DEFAULT_MAX_AGE);
	}

	public KeyLockableCache(ReferenceType type, Duration maxAge) {
		this(type, KeyLockableCache.MIN_LIMIT, maxAge);
	}

	public KeyLockableCache(ReferenceType type, int maxCount) {
		this(type, maxCount, KeyLockableCache.DEFAULT_MAX_AGE);
	}

	public KeyLockableCache(ReferenceType type, int maxCount, Duration maxAge) {

		switch (Tools.coalesce(type, KeyLockableCache.DEFAULT_TYPE)) {
			case WEAK:
				this.itemConstructor = WeakReferenceCacheItem::new;
				break;
			case SOFT:
				this.itemConstructor = SoftReferenceCacheItem::new;
				break;
			default:
			case FINAL:
				this.itemConstructor = DirectCacheItem::new;
				break;
		}

		this.cacheId = String.format("%016x", KeyLockableCache.CACHE_ID.getAndIncrement());
		this.cache = new LRUMap<>(Math.max(KeyLockableCache.MIN_LIMIT, maxCount));
		this.maxAge = maxAge;
	}

	public final ShareableLockable getLock(K key) {
		return this.locks.getLock(key);
	}

	protected CacheItem newCacheItem(K key, V value) {
		return this.itemConstructor.apply(key, value);
	}

	public final V get(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		// This construct helps avoid deadlocks while preserving concurrency where possible
		try (SharedAutoLock keyShared = getLock(key).autoSharedLock()) {
			try (SharedAutoLock cacheShared = autoSharedLock()) {
				CacheItem item = this.cache.get(key);
				return (item != null ? item.get() : null);
			}
		}
	}

	public final <EX extends Throwable> V computeIfAbsent(K key, CheckedFunction<K, V, EX> f) throws EX {
		Objects.requireNonNull(key, "Must provide a non-null key");
		Objects.requireNonNull(f, "Must provide a non-null initializer");
		try (SharedAutoLock keyShared = getLock(key).autoSharedLock()) {
			V existing = get(key);
			if (existing == null) {
				try (MutexAutoLock keyMutex = keyShared.upgrade()) {
					existing = get(key);
					if (existing == null) {
						try (SharedAutoLock cacheShared = autoSharedLock()) {
							CacheItem item = this.cache.get(key);
							existing = (item != null ? item.get() : null);
							if (existing != null) { return existing; }
							try (MutexAutoLock cacheMutex = cacheShared.upgrade()) {
								this.cache.put(key, newCacheItem(key, existing = f.applyChecked(key)));
							}
						}
					}
				}
			}
			return existing;
		}
	}

	public final V putIfAbsent(K key, V value) {
		return computeIfAbsent(key, (k) -> value);
	}

	public final V put(K key, V value) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		if (value == null) { return remove(key); }
		try (MutexAutoLock keyMutex = getLock(key).autoMutexLock()) {
			try (MutexAutoLock cacheMutex = autoMutexLock()) {
				CacheItem item = this.cache.put(key, newCacheItem(key, value));
				return (item != null ? item.get() : null);
			}
		}
	}

	public final V remove(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		try (MutexAutoLock keyMutex = getLock(key).autoMutexLock()) {
			try (SharedAutoLock cacheShared = autoSharedLock()) {
				CacheItem item = null;
				if (this.cache.containsKey(key)) {
					try (MutexAutoLock cacheMutex = cacheShared.upgrade()) {
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
		return shareLocked(this.cache::size);
	}

	public final boolean isEmpty() {
		return shareLocked(this.cache::isEmpty);
	}

	public final boolean containsValueForKey(K key) {
		Objects.requireNonNull(key, "Must provide a non-null key");
		try (SharedAutoLock keyShared = getLock(key).autoSharedLock()) {
			CacheItem item = null;
			try (SharedAutoLock cacheShared = autoSharedLock()) {
				item = this.cache.get(key);
			}
			if (item == null) { return false; }
			return (item.get() != null);
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
		mutexLocked(this.cache::clear);
	}

	public final Set<K> getKeys() {
		return shareLocked(() -> new LinkedHashSet<>(this.cache.keySet()));
	}
}