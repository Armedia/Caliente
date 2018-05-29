package com.armedia.caliente.store;

import java.util.Collection;

public class CmfAttribute<VALUE> extends CmfProperty<VALUE> {

	public CmfAttribute(CmfAttribute<VALUE> pattern) {
		super(pattern);
	}

	public CmfAttribute(CmfEncodeableName name, CmfDataType type, boolean repeating) {
		super(name, type, repeating);
	}

	public CmfAttribute(CmfEncodeableName name, CmfDataType type, boolean repeating, Collection<VALUE> values) {
		super(name, type, repeating, values);
	}

	public CmfAttribute(String name, CmfDataType type, boolean repeating) {
		super(name, type, repeating);
	}

	public CmfAttribute(String name, CmfDataType type, boolean repeating, Collection<VALUE> values) {
		super(name, type, repeating, values);
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
		return String.format("CmfAttribute [name=%s, type=%s, repeating=%s %s=%s]", getName(), getType(), isRepeating(),
			(isRepeating() ? "values" : "singleValue"), (isRepeating() ? getValues() : getValue()));
	}
}