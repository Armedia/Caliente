package com.armedia.cmf.engine.alfresco.bulk.importer.model;

import com.armedia.commons.utilities.Tools;

public final class SchemaAttribute {
	public final String name;
	public final AlfrescoDataType type;
	public final boolean multiple;
	public final SchemaMember<?> declaration;

	SchemaAttribute(SchemaMember<?> declaration, String name, AlfrescoDataType type) {
		this(declaration, name, type, false);
	}

	SchemaAttribute(SchemaMember<?> declaration, String name, AlfrescoDataType type, boolean multiple) {
		this.declaration = declaration;
		this.name = name;
		this.type = type;
		this.multiple = multiple;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.multiple, this.declaration);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		SchemaAttribute other = SchemaAttribute.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.multiple != other.multiple) { return false; }
		if (!Tools.equals(this.declaration, other.declaration)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("SchemaAttribute [name=%s, type=%s, multiple=%s, declaration=%s]", this.name, this.type,
			this.multiple, this.declaration.name);
	}
}