package com.armedia.caliente.engine.alfresco.bi.importer.model;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.MandatoryDef;
import com.armedia.commons.utilities.Tools;

public final class SchemaAttribute {

	public static enum Mandatory {
		//
		OPTIONAL, RELAXED, ENFORCED,
		//
		;
	}

	private static Mandatory decodeMandatory(MandatoryDef def) {
		if (def == null) { return Mandatory.OPTIONAL; }
		final boolean mandatory = Tools.coalesce(def.getValue(), Boolean.FALSE).booleanValue();
		if (!mandatory) { return Mandatory.OPTIONAL; }
		final boolean enforced = Tools.coalesce(def.getEnforced(), Boolean.FALSE).booleanValue();
		return (enforced ? Mandatory.ENFORCED : Mandatory.RELAXED);
	}

	public final String name;
	public final AlfrescoDataType type;
	public final boolean multiple;
	public final Mandatory mandatory;
	public final SchemaMember<?> declaration;

	SchemaAttribute(SchemaMember<?> declaration, String name, AlfrescoDataType type, MandatoryDef mandatory,
		boolean multiple) {
		this.declaration = declaration;
		this.name = name;
		this.type = type;
		this.mandatory = SchemaAttribute.decodeMandatory(mandatory);
		this.multiple = multiple;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.mandatory, this.multiple, this.declaration);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		SchemaAttribute other = SchemaAttribute.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.mandatory != other.mandatory) { return false; }
		if (this.multiple != other.multiple) { return false; }
		if (!Tools.equals(this.declaration, other.declaration)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("SchemaAttribute [name=%s, type=%s, mandatory=%s, multiple=%s, declaration=%s]", this.name,
			this.type, this.mandatory, this.multiple, this.declaration.name);
	}
}