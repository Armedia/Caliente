package com.armedia.caliente.engine.importer.schema.decl;

import java.util.Collection;

public class TypeDeclaration extends AttributeContainerDeclaration<TypeDeclaration> {

	public static final TypeDeclaration NULL = new TypeDeclaration();

	private TypeDeclaration() {
		super(null);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration<TypeDeclaration>> attributes,
		Collection<String> secondaries, String parent) {
		super(name, attributes, secondaries, parent);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration<TypeDeclaration>> attributes,
		Collection<String> secondaries) {
		super(name, attributes, secondaries);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration<TypeDeclaration>> attributes, String parent) {
		super(name, attributes, parent);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration<TypeDeclaration>> attributes) {
		super(name, attributes);
	}
}