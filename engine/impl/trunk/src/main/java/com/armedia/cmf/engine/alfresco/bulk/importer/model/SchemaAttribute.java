package com.armedia.cmf.engine.alfresco.bulk.importer.model;

public final class SchemaAttribute {
	public final String name;
	public final AlfrescoDataType type;
	public final boolean multiple;

	SchemaAttribute(String name, AlfrescoDataType type) {
		this(name, type, false);
	}

	SchemaAttribute(String name, AlfrescoDataType type, boolean multiple) {
		this.name = name;
		this.type = type;
		this.multiple = multiple;
	}
}