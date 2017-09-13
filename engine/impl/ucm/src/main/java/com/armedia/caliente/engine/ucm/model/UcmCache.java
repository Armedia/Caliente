package com.armedia.caliente.engine.ucm.model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;

import com.armedia.commons.utilities.LockDispenser;

public abstract class UcmCache<K, E> {
	private final CacheAccess<K, E> cache;
	private final LockDispenser<K, Lock> locks = new LockDispenser<K, Lock>() {
		@Override
		protected Lock newLock(K key) {
			return new ReentrantLock();
		}
	};

	public UcmCache(String name) {
		this.cache = JCS.getInstance(name);
	}

	public Lock getLock(K key) {
		return this.locks.getLock(key);
	}

	public abstract E initialize(K key);
}