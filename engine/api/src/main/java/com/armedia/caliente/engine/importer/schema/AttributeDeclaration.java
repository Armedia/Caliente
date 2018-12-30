package com.armedia.caliente.engine.importer.schema;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

public final class AttributeDeclaration {
	public final String name;
	public final CmfDataType type;
	public final boolean multiple;
	public final boolean required;

	public AttributeDeclaration(String name, CmfDataType type, boolean required, boolean multiple) {
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
		AttributeDeclaration other = AttributeDeclaration.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.required != other.required) { return false; }
		if (this.multiple != other.multiple) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("AttributeDeclaration [name=%s, type=%s, required=%s, multiple=%s]", this.name, this.type,
			this.required, this.multiple);
	}
}