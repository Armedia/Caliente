package com.armedia.cmf.storage;

import java.util.Collection;

public class StoredAttribute<V> extends StoredProperty<V> {

	public StoredAttribute(StoredAttribute<V> pattern) {
		super(pattern);
	}

	public StoredAttribute(String name, StoredDataType type, boolean repeating) {
		super(name, type, repeating);
	}

	public StoredAttribute(String name, StoredDataType type, boolean repeating, Collection<V> values) {
		super(name, type, repeating, values);
	}

	@Override
	public String toString() {
		return String.format("StoredAttribute [name=%s, type=%s, repeating=%s %s=%s]", getName(), getType(),
			isRepeating(), (isRepeating() ? "values" : "singleValue"), (isRepeating() ? getValues() : getValue()));
	}
}