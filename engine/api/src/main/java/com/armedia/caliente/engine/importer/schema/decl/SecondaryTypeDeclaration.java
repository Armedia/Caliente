package com.armedia.caliente.engine.importer.schema.decl;

import java.util.Collection;

public class SecondaryTypeDeclaration extends AttributeContainerDeclaration {

	public static final SecondaryTypeDeclaration NULL = new SecondaryTypeDeclaration();

	private SecondaryTypeDeclaration() {
		super(null);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration> attributes,
		Collection<String> secondaries, String parent) {
		super(name, attributes, secondaries, parent);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration> attributes,
		Collection<String> secondaries) {
		super(name, attributes, secondaries);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration> attributes, String parent) {
		super(name, attributes, parent);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration> attributes) {
		super(name, attributes);
	}
}