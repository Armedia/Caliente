package com.armedia.caliente.engine.schema;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

public final class SchemaAttribute {

	public final String name;
	public final CmfDataType type;
	public final boolean multiple;
	public final boolean required;
	public final SchemaMember<?> declaration;

	SchemaAttribute(SchemaMember<?> declaration, String name, CmfDataType type, boolean required, boolean multiple) {
		this.declaration = declaration;
		this.name = name;
		this.type = type;
		this.required = required;
		this.multiple = multiple;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.required, this.multiple, this.declaration);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		SchemaAttribute other = SchemaAttribute.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.required != other.required) { return false; }
		if (this.multiple != other.multiple) { return false; }
		if (!Tools.equals(this.declaration, other.declaration)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("SchemaAttribute [name=%s, type=%s, required=%s, multiple=%s, declaration=%s]", this.name,
			this.type, this.required, this.multiple, this.declaration.name);
	}
}