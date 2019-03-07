package com.armedia.caliente.engine.tools;

import org.apache.commons.collections4.map.LRUMap;

public class LRUCache<KEY, VALUE> extends ReadWriteMap<KEY, VALUE> {
	public LRUCache(int maxSize) {
		super(new LRUMap<>(maxSize));
	}
}