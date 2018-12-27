package com.armedia.caliente.engine.schema;

import java.util.Objects;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

public final class ObjectAttribute {

	public final SchemaMember<?> declaration;
	public final String name;
	public final CmfDataType type;
	public final boolean multiple;
	public final boolean required;

	ObjectAttribute(SchemaMember<?> declaration, String name, CmfDataType type, boolean required, boolean multiple) {
		this.declaration = Objects.requireNonNull(declaration,
			"Must provide the SchemaMember that declares this attribute");
		this.name = name;
		this.type = type;
		this.required = required;
		this.multiple = multiple;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.required, this.multiple);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ObjectAttribute other = ObjectAttribute.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.required != other.required) { return false; }
		if (this.multiple != other.multiple) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("ObjectAttribute [name=%s, type=%s, required=%s, multiple=%s]", this.name, this.type,
			this.required, this.multiple);
	}
}