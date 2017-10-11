package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AttributeNamesSource implements Iterable<String> {

	public static final Character DEFAULT_SEPARATOR = Character.valueOf(',');

	@XmlValue
	protected String value;

	@XmlAttribute(name = "caseSensitive")
	protected Boolean caseSensitive;

	@XmlTransient
	protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	private Boolean activeCaseSensitive = null;

	@XmlTransient
	private Map<String, String> values = null;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
		this.values = null;
	}

	public boolean isCaseSensitive() {
		return Tools.coalesce(this.caseSensitive, Boolean.FALSE);
	}

	public void setCaseSensitive(Boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	protected final String canonicalize(String value) {
		return (this.activeCaseSensitive ? value : StringUtils.upperCase(value));
	}

	protected abstract Set<String> getValues(Connection c) throws Exception;

	public final void initialize(Connection c) throws Exception {
		Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.values != null) { return; }
			this.activeCaseSensitive = isCaseSensitive();
			Set<String> values = getValues(c);
			if (values == null) {
				values = Collections.emptySet();
			}
			Map<String, String> m = new HashMap<>();
			for (String s : values) {
				m.put(canonicalize(s), s);
			}
			this.values = Tools.freezeMap(m);
		} finally {
			lock.unlock();
		}
	}

	public final int size() {
		Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			if (this.values == null) { return 0; }
			return this.values.size();
		} finally {
			lock.unlock();
		}
	}

	public final Set<String> getValues() {
		Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			if (this.values == null) { return Collections.emptySet(); }
			return Tools.freezeSet(new LinkedHashSet<>(this.values.values()));
		} finally {
			lock.unlock();
		}
	}

	public final Set<String> getCanonicalizedValues() {
		Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			if (this.values == null) { return Collections.emptySet(); }
			return Tools.freezeSet(new LinkedHashSet<>(this.values.keySet()));
		} finally {
			lock.unlock();
		}
	}

	public final boolean contains(String str) {
		Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			if (this.values == null) { return false; }
			return this.values.containsKey(canonicalize(str));
		} finally {
			lock.unlock();
		}
	}

	@Override
	public final Iterator<String> iterator() {
		return getValues().iterator();
	}

	public final void close() {
		Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.values == null) { return; }
			this.values = null;
			this.activeCaseSensitive = null;
		} finally {
			lock.unlock();
		}
	}
}