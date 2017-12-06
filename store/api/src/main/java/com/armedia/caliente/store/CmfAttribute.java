package com.armedia.caliente.store;

import java.util.Collection;

public class CmfAttribute<V> extends CmfProperty<V> {

	public CmfAttribute(CmfAttribute<V> pattern) {
		super(pattern);
	}

	public CmfAttribute(CmfEncodeableName name, CmfDataType type, boolean repeating) {
		super(name, type, repeating);
	}

	public CmfAttribute(CmfEncodeableName name, CmfDataType type, boolean repeating, Collection<V> values) {
		super(name, type, repeating, values);
	}

	public CmfAttribute(String name, CmfDataType type, boolean repeating) {
		super(name, type, repeating);
	}

	public CmfAttribute(String name, CmfDataType type, boolean repeating, Collection<V> values) {
		super(name, type, repeating, values);
	}

	@Override
	public CmfAttribute<V> setValues(Collection<V> values) {
		super.setValues(values);
		return this;
	}

	@Override
	public CmfAttribute<V> addValue(V value) {
		super.addValue(value);
		return this;
	}

	@Override
	public CmfAttribute<V> addValues(Collection<V> values) {
		super.addValues(values);
		return this;
	}

	@Override
	public CmfAttribute<V> setValue(V value) {
		super.setValue(value);
		return this;
	}

	@Override
	public String toString() {
		return String.format("CmfAttribute [name=%s, type=%s, repeating=%s %s=%s]", getName(), getType(), isRepeating(),
			(isRepeating() ? "values" : "singleValue"), (isRepeating() ? getValues() : getValue()));
	}
}