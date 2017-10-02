package com.armedia.caliente.engine.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfBaseSetting;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class TypedValue extends CmfBaseSetting {

	private final List<Object> values = new ArrayList<>();
	private Object value = null;

	public TypedValue(TypedValue member) {
		super(member);
		if (member.isRepeating()) {
			this.values.addAll(member.values);
		} else {
			this.value = member.value;
		}
	}

	public TypedValue(String name, CmfDataType type, boolean multivalue) {
		super(name, type, multivalue);
	}

	public TypedValue(CmfProperty<CmfValue> property) {
		super(property);
		// Copy the values over
		if (isRepeating()) {
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

	public <V> TypedValue(CmfProperty<V> property, CmfAttributeTranslator<V> translator) {
		super(property);
		// Copy the values over
		CmfValueCodec<V> codec = translator.getCodec(property.getType());
		if (isRepeating()) {
			for (V raw : property) {
				CmfValue v = codec.encodeValue(raw);
				if ((v != null) && !v.isNull()) {
					this.values.add(this.type.getValue(v));
				} else {
					this.values.add(null);
				}
			}
		} else {
			CmfValue v = codec.encodeValue(property.getValue());
			if ((v != null) && !v.isNull()) {
				this.value = this.type.getValue(v);
			} else {
				this.value = null;
			}
		}
	}

	public boolean isEmpty() {
		return (isRepeating() ? this.values.isEmpty() : false);
	}

	public Object getValue() {
		if (!isRepeating()) { return this.value; }
		List<Object> values = getValues();
		if (values.isEmpty()) { throw new IndexOutOfBoundsException(); }
		return values.get(0);
	}

	public TypedValue setValue(Object value) {
		if (!isRepeating()) {
			this.value = value;
		} else {
			this.values.clear();
			this.values.add(value);
		}
		return this;
	}

	public TypedValue setValues(Iterator<?> values) {
		if (!isRepeating()) {
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

	public TypedValue setValues(Iterable<?> values) {
		if (values != null) {
			values = Collections.emptyList();
		}
		setValues(values.iterator());
		return this;
	}

	public TypedValue setValues(Collection<?> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
		setValues(values.iterator());
		return this;
	}

	public List<Object> getValues() {
		if (!isRepeating()) {
			List<Object> ret = new ArrayList<>(1);
			ret.add(this.value);
			return ret;
		}
		return this.values;
	}

	public int getSize() {
		return (!isRepeating() ? 1 : this.values.size());
	}

	@Override
	public String toString() {
		return Tools.toString(isRepeating() ? this.values : this.value);
	}
}