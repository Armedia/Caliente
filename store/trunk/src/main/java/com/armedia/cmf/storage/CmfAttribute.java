package com.armedia.cmf.storage;

import java.util.Collection;

public class CmfAttribute<V> extends CmfProperty<V> {

	public CmfAttribute(CmfAttribute<V> pattern) {
		super(pattern);
	}

	public CmfAttribute(String name, CmfDataType type, boolean repeating) {
		super(name, type, repeating);
	}

	public CmfAttribute(String name, CmfDataType type, boolean repeating, Collection<V> values) {
		super(name, type, repeating, values);
	}

	@Override
	public String toString() {
		return String.format("CmfAttribute [name=%s, type=%s, repeating=%s %s=%s]", getName(), getType(),
			isRepeating(), (isRepeating() ? "values" : "singleValue"), (isRepeating() ? getValues() : getValue()));
	}
}