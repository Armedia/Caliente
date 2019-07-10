/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.dynamic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfBaseSetting;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class DynamicValue extends CmfBaseSetting {

	private final List<Object> values = new ArrayList<>();
	private Object value = null;

	public DynamicValue(DynamicValue member) {
		super(member);
		if (member.isMultivalued()) {
			this.values.addAll(member.values);
		} else {
			this.value = member.value;
		}
	}

	public DynamicValue(String name, CmfValue.Type type, boolean multivalue) {
		super(name, type, multivalue);
	}

	public <V extends CmfProperty<CmfValue>> DynamicValue(V property) {
		super(property);
		// Copy the values over
		if (isMultivalued()) {
			for (CmfValue v : property) {
				if ((v != null) && !v.isNull()) {
					this.values.add(this.type.getValue(v));
				} else {
					this.values.add(null);
				}
			}
		} else {
			CmfValue v = property.getValue();
			if ((v != null) && !v.isNull()) {
				this.value = this.type.getValue(v);
			} else {
				this.value = null;
			}
		}
	}

	public <V> DynamicValue(CmfProperty<V> property, CmfAttributeTranslator<V> translator) {
		super(property);
		// Copy the values over
		CmfValueCodec<V> codec = translator.getCodec(property.getType());
		if (isMultivalued()) {
			for (V raw : property) {
				CmfValue v = codec.encode(raw);
				if ((v != null) && !v.isNull()) {
					this.values.add(this.type.getValue(v));
				} else {
					this.values.add(null);
				}
			}
		} else {
			CmfValue v = codec.encode(property.getValue());
			if ((v != null) && !v.isNull()) {
				this.value = this.type.getValue(v);
			} else {
				this.value = null;
			}
		}
	}

	public boolean isEmpty() {
		Object value = null;
		if (isMultivalued()) {
			// it will be empty if and only if it has no values, or its first value is a
			// an empty value in the single-valued sense
			if (this.values.isEmpty()) { return true; }
			if (this.values.size() > 1) { return false; }
			value = this.values.get(0);
		} else {
			value = this.value;
		}
		if (value == null) { return true; }
		return StringUtils.isEmpty(Tools.toString(value));
	}

	public Object getValue() {
		if (!isMultivalued()) { return this.value; }
		List<Object> values = getValues();
		if (values.isEmpty()) { throw new IndexOutOfBoundsException(); }
		return values.get(0);
	}

	public DynamicValue setValue(Object value) {
		if (!isMultivalued()) {
			this.value = value;
		} else {
			this.values.clear();
			this.values.add(value);
		}
		return this;
	}

	public DynamicValue setValues(Iterator<?> values) {
		if (!isMultivalued()) {
			this.value = null;
			if ((values != null) && values.hasNext()) {
				this.value = values.next();
			} else {
				this.value = null;
			}
		} else {
			this.values.clear();
			if (values == null) {
				values = Collections.emptyList().iterator();
			}
			while (values.hasNext()) {
				this.values.add(values.next());
			}
		}
		return this;
	}

	public DynamicValue setValues(Iterable<?> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
		setValues(values.iterator());
		return this;
	}

	public DynamicValue setValues(Collection<?> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
		setValues(values.iterator());
		return this;
	}

	public List<Object> getValues() {
		if (!isMultivalued()) {
			List<Object> ret = new ArrayList<>(1);
			ret.add(this.value);
			return ret;
		}
		return this.values;
	}

	public int getSize() {
		return (!isMultivalued() ? 1 : this.values.size());
	}

	@Override
	public String toString() {
		return Tools.toString(isMultivalued() ? this.values : this.value);
	}
}