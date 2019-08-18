package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.dynamic.ScriptablePropertyFacade;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfBaseSetting;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

class MetadataPropertyFacade extends CmfBaseSetting implements ScriptablePropertyFacade {

	private final List<Object> values;
	private Object value = null;

	<V> MetadataPropertyFacade(CmfProperty<V> property, CmfAttributeTranslator<V> translator) {
		super(property);
		// Copy the values over
		CmfValueCodec<V> codec = translator.getCodec(property.getType());
		if (isMultivalued()) {
			List<Object> values = new ArrayList<>();
			for (V raw : property) {
				CmfValue v = codec.encode(raw);
				if ((v != null) && !v.isNull()) {
					values.add(this.type.getValue(v));
				} else {
					values.add(null);
				}
			}
			this.values = Tools.freezeList(values);
		} else {
			CmfValue v = codec.encode(property.getValue());
			if ((v != null) && !v.isNull()) {
				this.value = this.type.getValue(v);
			} else {
				this.value = null;
			}
			this.values = Collections.emptyList();
		}
	}

	public boolean isEmpty() {
		if (isMultivalued()) { return this.values.isEmpty(); }
		return (this.value == null);
	}

	public Object getValue() {
		if (!isMultivalued()) { return this.value; }
		if (this.values.isEmpty()) { throw new IndexOutOfBoundsException(); }
		return this.values.get(0);
	}

	public List<Object> getValues() {
		if (!isMultivalued()) { return Collections.unmodifiableList(Collections.singletonList(this.value)); }
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