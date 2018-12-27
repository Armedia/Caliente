package com.armedia.caliente.engine.schema.decl;

import java.util.Collection;

public class SecondaryTypeDeclaration extends AttributeContainerDeclaration<SecondaryTypeDeclaration> {

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration> attributes, Collection<String> secondaries,
		SecondaryTypeDeclaration parent) {
		super(name, attributes, secondaries, parent);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration> attributes, Collection<String> secondaries) {
		super(name, attributes, secondaries);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration> attributes, SecondaryTypeDeclaration parent) {
		super(name, attributes, parent);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration> attributes) {
		super(name, attributes);
	}
}