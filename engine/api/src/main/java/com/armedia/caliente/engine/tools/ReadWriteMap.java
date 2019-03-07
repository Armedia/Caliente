package com.armedia.caliente.engine.tools;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.armedia.commons.utilities.Tools;

public class ReadWriteMap<KEY, VALUE> extends BaseReadWriteLockable implements Map<KEY, VALUE> {

	protected final Map<KEY, VALUE> map;
	protected final Set<KEY> keys;
	protected final Set<Map.Entry<KEY, VALUE>> entries;
	protected final Collection<VALUE> values;

	public ReadWriteMap(Map<KEY, VALUE> map) {
		this(null, map);
	}

	public ReadWriteMap(ReadWriteLock rwLock, Map<KEY, VALUE> map) {
		super(rwLock);
		this.map = Objects.requireNonNull(map, "Must provide a non-null backing map");
		this.keys = new ReadWriteSet<>(rwLock, map.keySet());
		this.entries = new ReadWriteSet<>(rwLock, map.entrySet());
		this.values = new ReadWriteCollection<>(rwLock, map.values());
	}

	protected <K> K validateKey(K key) {
		return Objects.requireNonNull(key, "Must provide a non-null key");
	}

	protected <V> V validateValue(V value) {
		return Objects.requireNonNull(value, "Must provide a non-null value");
	}

	@Override
	public int size() {
		return readLocked(this.map::size);
	}

	@Override
	public boolean isEmpty() {
		return readLocked(this.map::isEmpty);
	}

	@Override
	public boolean containsKey(Object key) {
		Object K = validateKey(key);
		return readLocked(() -> {
			return this.map.containsKey(K);
		});
	}

	@Override
	public boolean containsValue(Object value) {
		Object V = validateValue(value);
		return readLocked(() -> {
			return this.map.containsValue(V);
		});
	}

	@Override
	public VALUE get(Object key) {
		Object K = validateKey(key);
		return readLocked(() -> {
			return this.map.get(K);
		});
	}

	@Override
	public VALUE put(KEY key, VALUE value) {
		KEY K = validateKey(key);
		VALUE V = validateValue(value);
		return writeLocked(() -> {
			return this.map.put(K, V);
		});
	}

	@Override
	public VALUE remove(Object key) {
		Object K = validateKey(key);
		return writeLocked(() -> {
			return this.map.remove(K);
		});
	}

	@Override
	public void putAll(Map<? extends KEY, ? extends VALUE> m) {
		Objects.requireNonNull(m, "Must provide a non-null map to put elements from");
		writeLocked(() -> {
			this.map.putAll(m);
		});
	}

	@Override
	public void clear() {
		writeLocked(this.map::clear);
	}

	@Override
	public Set<KEY> keySet() {
		return this.keys;
	}

	@Override
	public Collection<VALUE> values() {
		return this.values;
	}

	@Override
	public Set<Entry<KEY, VALUE>> entrySet() {
		return this.entries;
	}

	@Override
	public boolean equals(Object o) {
		return readLocked(() -> {
			if (o == null) { return false; }
			if (o == this) { return true; }
			if (!Map.class.isInstance(o)) { return false; }
			Map<?, ?> m = Map.class.cast(o);
			if (this.map.size() != m.size()) { return false; }
			return this.map.equals(o);
		});
	}

	@Override
	public int hashCode() {
		return readLocked(() -> {
			return Tools.hashTool(this, null, this.map);
		});
	}

	@Override
	public VALUE getOrDefault(Object key, VALUE defaultValue) {
		return readLocked(() -> {
			return this.map.getOrDefault(key, defaultValue);
		});
	}

	@Override
	public void forEach(BiConsumer<? super KEY, ? super VALUE> action) {
		readLocked(() -> {
			this.map.forEach(action);
		});
	}

	@Override
	public void replaceAll(BiFunction<? super KEY, ? super VALUE, ? extends VALUE> function) {
		writeLocked(() -> {
			this.map.replaceAll(function);
		});
	}

	@Override
	public VALUE putIfAbsent(KEY key, VALUE value) {
		KEY K = validateKey(key);
		VALUE V = validateValue(value);

		return readUpgradable(() -> {
			return this.map.get(K);
		}, Objects::nonNull, (e) -> {
			this.map.put(K, V);
			return null;
		});
	}

	@Override
	public boolean remove(Object key, Object value) {
		Object K = validateKey(key);
		Object V = validateValue(value);
		return writeLocked(() -> {
			return this.map.remove(K, V);
		});
	}

	@Override
	public boolean replace(KEY key, VALUE oldValue, VALUE newValue) {
		KEY K = validateKey(key);
		VALUE O = validateValue(oldValue);
		VALUE N = validateValue(newValue);
		return writeLocked(() -> {
			return this.map.replace(K, O, N);
		});
	}

	@Override
	public VALUE replace(KEY key, VALUE value) {
		KEY K = validateKey(key);
		VALUE V = validateValue(value);
		return writeLocked(() -> {
			return this.map.replace(K, V);
		});
	}

	@Override
	public VALUE computeIfAbsent(KEY key, Function<? super KEY, ? extends VALUE> mappingFunction) {
		Objects.requireNonNull(mappingFunction, "Must provide a non-null mapping function");
		final KEY K = validateKey(key);
		return readUpgradable(() -> {
			return this.map.get(K);
		}, Objects::nonNull, (V) -> {
			V = mappingFunction.apply(K);
			if (V != null) {
				this.map.put(K, V);
			}
			return V;
		});
	}

	@Override
	public VALUE computeIfPresent(KEY key, BiFunction<? super KEY, ? super VALUE, ? extends VALUE> remappingFunction) {
		Objects.requireNonNull(remappingFunction, "Must provide a non-null remapping function");
		final KEY K = validateKey(key);
		return readUpgradable(() -> {
			return this.map.get(K);
		}, Objects::isNull, (V) -> {
			V = remappingFunction.apply(K, V);
			if (V != null) {
				this.map.put(K, V);
			} else {
				this.map.remove(K);
			}
			return V;
		});
	}

	@Override
	public VALUE compute(KEY key, BiFunction<? super KEY, ? super VALUE, ? extends VALUE> remappingFunction) {
		Objects.requireNonNull(remappingFunction, "Must provide a non-null remapping function");
		KEY K = validateKey(key);
		return writeLocked(() -> {
			return this.map.compute(K, remappingFunction);
		});
	}

	@Override
	public VALUE merge(KEY key, VALUE value,
		BiFunction<? super VALUE, ? super VALUE, ? extends VALUE> remappingFunction) {
		Objects.requireNonNull(remappingFunction, "Must provide a non-null remapping function");
		KEY K = validateKey(key);
		VALUE V = validateValue(value);
		return writeLocked(() -> {
			return this.map.merge(K, V, remappingFunction);
		});
	}
}