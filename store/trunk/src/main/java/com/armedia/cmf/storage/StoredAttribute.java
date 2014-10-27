package com.armedia.cmf.storage;

import java.util.Collection;

import com.armedia.commons.utilities.Tools;

public class StoredAttribute<V> extends StoredProperty<V> {

	private final String id;
	private final boolean qualifiable;
	private final int length;

	public StoredAttribute(String name, StoredDataType type, String id, int length, boolean repeating,
		boolean qualifiable) {
		this(name, type, id, length, repeating, qualifiable, null);
	}

	public StoredAttribute(String name, StoredDataType type, String id, int length, boolean repeating,
		boolean qualifiable, Collection<V> values) {
		super(name, type, repeating, values);
		if (id == null) { throw new IllegalArgumentException("Must provide a non-null attribute id"); }
		this.id = id;
		this.length = length;
		this.qualifiable = qualifiable;
	}

	public final boolean isQualifiable() {
		return this.qualifiable;
	}

	public final String getId() {
		return this.id;
	}

	public final int getLength() {
		return this.length;
	}

	@Override
	public final boolean isSame(StoredProperty<?> other) {
		if (!super.isSame(other)) { return false; }
		if (!(other instanceof StoredAttribute)) { return false; }
		StoredAttribute<?> o = StoredAttribute.class.cast(other);
		if (!Tools.equals(this.id, o.id)) { return false; }
		if (this.qualifiable != o.qualifiable) { return false; }
		if (this.length != o.length) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"StoredAttribute [name=%s, type=%s, id=%s, repeating=%s, qualifiable=%s, length=%d %s=%s]", getName(),
			getType(), this.id, isRepeating(), this.qualifiable, this.length,
			(isRepeating() ? "values" : "singleValue"), (isRepeating() ? getValues() : getValue()));
	}
}