package com.armedia.cmf.engine.alfresco.bulk.importer.model;

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
}