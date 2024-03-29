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
package com.armedia.caliente.engine.ucm.model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataResultSet.Field;
import oracle.stellent.ridc.model.DataResultSet.Field.Type;
import oracle.stellent.ridc.model.impl.DataObjectEncodingUtils;

public final class UcmAttributes {

	private static final Set<String> TRUE_VALUES;
	static {
		Set<String> s = new TreeSet<>();
		String[] v = {
			"1", "yes", "true", "on"
		};
		for (String str : v) {
			s.add(str);
		}
		TRUE_VALUES = Tools.freezeSet(s);
	}

	private final Map<String, CmfValue> data;

	private static Collection<Field> calculateStructure(DataBinder binder) {
		List<Field> ret = new ArrayList<>(binder.getFieldTypeNames().size());
		for (String field : binder.getFieldTypeNames()) {
			String type = binder.getFieldType(field);
			if (type != null) {
				Type t = null;
				try {
					t = Type.valueOf(type.toUpperCase());
				} catch (IllegalArgumentException e) {
					// Default to string...
					t = Type.STRING;
				}
				Field f = new Field(field);
				f.setType(t);
				ret.add(f);
			}
		}
		return ret;
	}

	public UcmAttributes(Map<String, String> data, DataBinder binder) {
		this(data, UcmAttributes.calculateStructure(binder));
	}

	@SafeVarargs
	public UcmAttributes(Map<String, String> data, Collection<Field>... structures) {
		if (data == null) {
			data = new TreeMap<>();
		} else {
			data = new TreeMap<>(data);
		}
		Map<String, CmfValue> tgt = new TreeMap<>();
		for (Collection<Field> structure : structures) {
			if (structure == null) {
				continue;
			}
			for (Field f : structure) {
				String v = data.get(f.getName());

				Type t = f.getType();
				// Map that type to a CmfValue.Type
				CmfValue value = null;
				CmfValue.Type T = null;
				switch (t) {
					case BOOLEAN:
						if (!StringUtils.isEmpty(v)) {
							value = CmfValue.of(UcmAttributes.TRUE_VALUES.contains(v.toString().toUpperCase()));
						} else {
							T = CmfValue.Type.BOOLEAN;
						}
						break;
					case CHAR:
					case STRING:
					case MEMO:
					case CLOB:
						if (v != null) {
							value = CmfValue.of(v.toString());
						} else {
							T = CmfValue.Type.STRING;
						}
						break;
					case DECIMAL:
					case FLOAT:
						// Parse as a double...
						if (!StringUtils.isEmpty(v)) {
							value = CmfValue.of(Double.valueOf(v.toString()));
						} else {
							T = CmfValue.Type.DOUBLE;
						}
						break;
					case DATE:
						if (!StringUtils.isEmpty(v)) {
							try {
								value = CmfValue.of(DataObjectEncodingUtils.decodeDate(v.toString()));
							} catch (ParseException e) {
								throw new UcmRuntimeException(
									String.format("Failed to parse the value [%s] as a date", v), e);
							}
						} else {
							T = CmfValue.Type.DATETIME;
						}
						break;
					case INT:
						if (!StringUtils.isEmpty(v)) {
							value = CmfValue.of(Long.valueOf(v.toString()));
						} else {
							T = CmfValue.Type.INTEGER;
						}
						break;
					case BINARY:
					case BLOB:
						// Can't support this...no way to retrieve the data, and even if I got it as
						// a string, I don't know how it's encoded and thus how to turn it into a
						// binary stream of octets...so we simply keep a null value in its place
						T = CmfValue.Type.BASE64_BINARY;
						break;
				}
				if (value == null) {
					value = T.getNull();
				}
				tgt.put(f.getName(), value);
			}
		}
		// Now, all the other custom fields are string-typed
		for (String s : data.keySet()) {
			if (!tgt.containsKey(s)) {
				tgt.put(s, CmfValue.of(data.get(s)));
			}
		}
		this.data = tgt;
	}

	Map<String, CmfValue> getMutableData() {
		return this.data;
	}

	public Map<String, CmfValue> getData() {
		return Collections.unmodifiableMap(this.data);
	}

	private String getKey(UcmAtt att) {
		Objects.requireNonNull(att, "Must provide a non-null attribute key");
		return att.name();
	}

	public CmfValue.Type getAttributeType(String name) {
		Objects.requireNonNull(name, "Must provide a non-null attribute name");
		CmfValue v = this.data.get(name);
		if (v == null) { return null; }
		return v.getDataType();
	}

	public CmfValue.Type getAttributeType(UcmAtt att) {
		return getAttributeType(getKey(att));
	}

	public boolean hasAttribute(String name) {
		Objects.requireNonNull(name, "Must provide a non-null attribute name");
		return this.data.containsKey(name);
	}

	public boolean hasAttribute(UcmAtt att) {
		return hasAttribute(getKey(att));
	}

	public CmfValue getValue(String name) {
		Objects.requireNonNull(name, "Must provide a non-null attribute name");
		return this.data.get(name);
	}

	public CmfValue getValue(UcmAtt att) {
		return getValue(getKey(att));
	}

	public String getString(String name, String def) {
		CmfValue v = getValue(name);
		if ((v == null) || v.isNull()) { return def; }
		return Tools.coalesce(v.asString(), def);
	}

	public String getString(String name) {
		return getString(name, null);
	}

	public String getString(UcmAtt att, String def) {
		return getString(getKey(att), def);
	}

	public String getString(UcmAtt att) {
		return getString(getKey(att), null);
	}

	public Date getDate(String name, Date def) throws ParseException {
		CmfValue v = this.getValue(name);
		if ((v == null) || v.isNull()) { return def; }
		return v.asTime();
	}

	public Date getDate(String name) throws ParseException {
		return getDate(name, null);
	}

	public Date getDate(UcmAtt att, Date def) throws ParseException {
		return getDate(getKey(att), def);
	}

	public Date getDate(UcmAtt att) throws ParseException {
		return getDate(getKey(att), null);
	}

	public long getLong(String name, long def) {
		Long v = getLong(name);
		return (v != null ? v.longValue() : def);
	}

	public Long getLong(String name) {
		CmfValue v = getValue(name);
		if ((v == null) || v.isNull()) { return null; }
		return v.asLong();
	}

	public long getLong(UcmAtt att, long def) {
		return getLong(getKey(att), def);
	}

	public Long getLong(UcmAtt att) {
		return getLong(getKey(att));
	}

	public int getInteger(String name, int def) {
		Integer v = getInteger(name);
		return (v != null ? v.intValue() : def);
	}

	public Integer getInteger(String name) {
		CmfValue v = getValue(name);
		if ((v == null) || v.isNull()) { return null; }
		return v.asInteger();
	}

	public int getInteger(UcmAtt att, int def) {
		return getInteger(getKey(att), def);
	}

	public Integer getInteger(UcmAtt att) {
		return getInteger(getKey(att));
	}

	public double getDouble(String name, double def) {
		Double v = getDouble(name);
		return (v != null ? v.doubleValue() : def);
	}

	public Double getDouble(String name) {
		CmfValue v = getValue(name);
		if ((v == null) || v.isNull()) { return null; }
		return v.asDouble();
	}

	public double getDouble(UcmAtt att, double def) {
		return getDouble(getKey(att), def);
	}

	public Double getDouble(UcmAtt att) {
		return getDouble(getKey(att));
	}

	public boolean getBoolean(String name, boolean def) {
		Boolean b = getBoolean(name);
		return (b != null ? b.booleanValue() : def);
	}

	public Boolean getBoolean(String name) {
		CmfValue v = getValue(name);
		if ((v == null) || v.isNull()) { return null; }
		return v.asBoolean();
	}

	public boolean getBoolean(UcmAtt att, boolean def) {
		return getBoolean(getKey(att), def);
	}

	public Boolean getBoolean(UcmAtt att) {
		return getBoolean(getKey(att));
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

	@Override
	public String toString() {
		return this.data.toString();
	}
}