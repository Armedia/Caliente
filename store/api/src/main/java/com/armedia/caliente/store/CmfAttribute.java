package com.armedia.caliente.store;

import java.util.Collection;

public class CmfAttribute<VALUE> extends CmfProperty<VALUE> {

	public CmfAttribute(CmfAttribute<VALUE> pattern) {
		super(pattern);
	}

	public CmfAttribute(CmfEncodeableName name, CmfValue.Type type, boolean multivalue) {
		super(name, type, multivalue);
	}

	public CmfAttribute(CmfEncodeableName name, CmfValue.Type type, boolean multivalue, Collection<VALUE> values) {
		super(name, type, multivalue, values);
	}

	public CmfAttribute(String name, CmfValue.Type type, boolean multivalue) {
		super(name, type, multivalue);
	}

	public CmfAttribute(String name, CmfValue.Type type, boolean multivalue, Collection<VALUE> values) {
		super(name, type, multivalue, values);
	}

	@Override
	public CmfAttribute<VALUE> setValues(Collection<VALUE> values) {
		super.setValues(values);
		return this;
	}

	@Override
	public CmfAttribute<VALUE> addValue(VALUE value) {
		super.addValue(value);
		return this;
	}

	@Override
	public CmfAttribute<VALUE> addValues(Collection<VALUE> values) {
		super.addValues(values);
		return this;
	}

	@Override
	public CmfAttribute<VALUE> setValue(VALUE value) {
		super.setValue(value);
		return this;
	}

	@Override
	public String toString() {
		return String.format("CmfAttribute [name=%s, type=%s, repeating=%s %s=%s]", getName(), getType(), isMultivalued(),
			(isMultivalued() ? "values" : "singleValue"), (isMultivalued() ? getValues() : getValue()));
	}
}