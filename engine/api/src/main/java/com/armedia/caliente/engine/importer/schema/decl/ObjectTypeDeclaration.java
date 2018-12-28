package com.armedia.caliente.engine.importer.schema.decl;

import java.util.Collection;

public class ObjectTypeDeclaration extends AttributeContainerDeclaration {

	public static final ObjectTypeDeclaration NULL = new ObjectTypeDeclaration();

	private ObjectTypeDeclaration() {
		super(null);
	}

	public ObjectTypeDeclaration(String name, Collection<AttributeDeclaration> attributes, Collection<String> secondaries,
		String parent) {
		super(name, attributes, secondaries, parent);
	}

	public ObjectTypeDeclaration(String name, Collection<AttributeDeclaration> attributes, Collection<String> secondaries) {
		super(name, attributes, secondaries);
	}

	public ObjectTypeDeclaration(String name, Collection<AttributeDeclaration> attributes, String parent) {
		super(name, attributes, parent);
	}

	public ObjectTypeDeclaration(String name, Collection<AttributeDeclaration> attributes) {
		super(name, attributes);
	}
}