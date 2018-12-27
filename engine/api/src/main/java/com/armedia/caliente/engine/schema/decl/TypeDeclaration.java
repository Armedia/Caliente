package com.armedia.caliente.engine.schema.decl;

import java.util.Collection;

public class TypeDeclaration extends AttributeContainerDeclaration<TypeDeclaration> {

	public TypeDeclaration(String name, Collection<AttributeDeclaration> attributes, Collection<String> secondaries,
		TypeDeclaration parent) {
		super(name, attributes, secondaries, parent);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration> attributes, Collection<String> secondaries) {
		super(name, attributes, secondaries);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration> attributes, TypeDeclaration parent) {
		super(name, attributes, parent);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration> attributes) {
		super(name, attributes);
	}
}