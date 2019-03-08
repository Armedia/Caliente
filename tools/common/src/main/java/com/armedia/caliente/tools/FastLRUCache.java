package com.armedia.caliente.tools;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.armedia.commons.utilities.function.CheckedSupplier;

public class FastLRUCache<KEY, VALUE> {

	@FunctionalInterface
	public static interface Evictable {
		public void evicted();
	}

	private class MapNode {
		private MapNode prev = null;
		private MapNode next = null;
		private long lastUsedNs = 0;

		final KEY key;
		final VALUE value;
		final Evictable evictable;

		private MapNode(KEY key, VALUE value) {
			this.key = key;
			this.value = value;
			if (Evictable.class.isInstance(value)) {
				this.evictable = Evictable.class.cast(value);
			} else {
				this.evictable = null;
			}
			this.lastUsedNs = System.nanoTime();
		}

		private void refresh() {
			FastLRUCache.this.write.lock();
			try {
				this.lastUsedNs = System.nanoTime();

				// Shortcuit, avoid work...
				if (FastLRUCache.this.tail == this) { return; }

				// First, disconnect...
				if (this.next != null) {
					this.next.prev = this.prev;
					this.next = null;
				}
				if (this.prev != null) {
					this.prev.next = this.next;
					this.prev = null;
				}

				// Then, reconnect at the tail...
				if (FastLRUCache.this.tail != null) {
					FastLRUCache.this.tail.next = this;
					this.prev = FastLRUCache.this.tail;
				} else {
					// No other elements
					FastLRUCache.this.head = this;
					FastLRUCache.this.tail = this;
				}
				FastLRUCache.this.tail = this;
			} finally {
				FastLRUCache.this.write.unlock();
			}
		}

		private void evict() {
			if (this.evictable != null) {
				this.evictable.evicted();
			}
		}
	}

	private final ConcurrentMap<KEY, MapNode> map = new ConcurrentHashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = this.lock.readLock();
	private final Lock write = this.lock.writeLock();
	private final int maxCount = 0;

	private MapNode head = null;
	private MapNode tail = null;

	public FastLRUCache() {

	}

	protected KEY validateKey(KEY key) {
		return Objects.requireNonNull(key, "Must provide a non-null key");
	}

	protected VALUE validateValue(VALUE value) {
		return Objects.requireNonNull(value, "Must provide a non-null value");
	}

	/**
	 * Put the given value into the cache, replacing any existing value
	 *
	 * @param key
	 * @param value
	 * @return the old value, if any
	 */
	public VALUE put(KEY key, VALUE value) {
		MapNode node = new MapNode(validateKey(key), validateValue(value));
		MapNode old = this.map.put(node.key, node);
		return (old != null ? old.value : null);
	}

	/**
	 * Analogous to
	 * {@link ConcurrentUtils#createIfAbsent(ConcurrentMap, Object, ConcurrentInitializer)}, but
	 * will invoke {@link CheckedSupplier#getChecked()} if necessary to calculate the value to be
	 * returned.
	 *
	 * @param key
	 * @param supplier
	 * @return the calculated value for the key
	 * @throws Exception
	 */
	public VALUE createIfAbsent(KEY key, CheckedSupplier<VALUE> supplier) throws Exception {
		return null;
	}

	/**
	 * Analogous to
	 * {@link ConcurrentUtils#createIfAbsentUnchecked(ConcurrentMap, Object, ConcurrentInitializer)},
	 * but will invoke {@link Supplier#get()} if necessary to calculate the value to be returned.
	 *
	 * @param key
	 * @param supplier
	 * @return the calculated value for the key
	 */
	public VALUE createIfAbsent(KEY key, final Supplier<VALUE> supplier) {
		AtomicReference<MapNode> created = new AtomicReference<>();
		final KEY finalKey = validateKey(key);
		MapNode value = ConcurrentUtils.createIfAbsentUnchecked(this.map, finalKey, () -> {
			VALUE v = validateValue(supplier.get());
			MapNode n = new MapNode(finalKey, v);
			created.set(n);
			return n;
		});
		value.refresh();
		return value.value;
	}

	public VALUE get(KEY key) {
		return null;
	}
}