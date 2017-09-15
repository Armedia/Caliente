package com.armedia.caliente.engine.ucm.model;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.model.impl.DataObjectEncodingUtils;

public final class UcmAttributes {

	private final Map<String, String> data;

	UcmAttributes(Map<String, String> data) {
		if (data == null) {
			data = new HashMap<>();
		} else {
			data = new HashMap<>(data);
		}
		this.data = data;
	}

	Map<String, String> getMutableData() {
		return this.data;
	}

	public Map<String, String> getData() {
		return Collections.unmodifiableMap(this.data);
	}

	private String getKey(UcmAtt att) {
		Objects.requireNonNull(att, "Must provide a non-null attribute key");
		return att.name();
	}

	public boolean hasAttribute(String name) {
		Objects.requireNonNull(name, "Must provide a non-null attribute name");
		return this.data.containsKey(name);
	}

	public String getString(String name) {
		return getString(name, null);
	}

	public String getString(String name, String def) {
		Objects.requireNonNull(name, "Must provide a non-null attribute name");
		String ret = this.data.get(name);
		return Tools.coalesce(ret, def);
	}

	public boolean hasAttribute(UcmAtt att) {
		return hasAttribute(getKey(att));
	}

	public String getString(UcmAtt att) {
		return getString(getKey(att));
	}

	public String getString(UcmAtt att, String def) {
		return getString(getKey(att), def);
	}

	public Date getDate(String name) {
		return getDate(name, null);
	}

	public Date getDate(String name, Date def) {
		Calendar c = getCalendar(name);
		if (c == null) { return def; }
		return c.getTime();
	}

	public Date getDate(UcmAtt att) {
		return getDate(getKey(att));
	}

	public Date getDate(UcmAtt att, Date def) {
		return getDate(getKey(att), def);
	}

	public Calendar getCalendar(String name) {
		return getCalendar(name, null);
	}

	public Calendar getCalendar(String name, Calendar def) {
		String v = getString(name);
		if (v == null) { return def; }
		try {
			return DataObjectEncodingUtils.decodeDate(v);
		} catch (ParseException e) {
			throw new UcmRuntimeException(String.format("Failed to parse out the calendar [%s]", v), e);
		}
	}

	public Calendar getCalendar(UcmAtt att) {
		return getCalendar(getKey(att));
	}

	public Calendar getCalendar(UcmAtt att, Calendar def) {
		return getCalendar(getKey(att), def);
	}

	public Integer getInteger(String name) {
		String str = getString(name);
		if (str == null) { return null; }
		return Integer.valueOf(str);
	}

	public int getInteger(String name, int def) {
		Integer v = getInteger(name);
		return (v != null ? v.intValue() : def);
	}

	public Integer getInteger(UcmAtt att) {
		return getInteger(getKey(att));
	}

	public int getInteger(UcmAtt att, int def) {
		return getInteger(getKey(att), def);
	}

	public Boolean getBoolean(String name) {
		return Tools.toBoolean(getString(name));
	}

	public boolean getBoolean(String name, boolean def) {
		Boolean b = getBoolean(name);
		return (b != null ? b.booleanValue() : def);
	}

	public Boolean getBoolean(UcmAtt att) {
		return getBoolean(getKey(att));
	}

	public boolean getBoolean(UcmAtt att, boolean def) {
		return getBoolean(getKey(att), def);
	}

	public Set<String> getValueNames() {
		return new TreeSet<>(this.data.keySet());
	}

	public Set<UcmAtt> getAttributes() {
		Set<UcmAtt> ret = EnumSet.noneOf(UcmAtt.class);
		for (String s : this.data.keySet()) {
			try {
				ret.add(UcmAtt.valueOf(s));
			} catch (IllegalArgumentException e) {
				// Do nothing...
			}
		}
		return ret;
	}
}