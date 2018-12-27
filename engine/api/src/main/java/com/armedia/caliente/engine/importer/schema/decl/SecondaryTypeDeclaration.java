package com.armedia.caliente.engine.importer.schema.decl;

import java.util.Collection;

public class SecondaryTypeDeclaration extends AttributeContainerDeclaration<SecondaryTypeDeclaration> {

	public static final SecondaryTypeDeclaration NULL = new SecondaryTypeDeclaration();

	private SecondaryTypeDeclaration() {
		super(null);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration<SecondaryTypeDeclaration>> attributes,
		Collection<String> secondaries, String parent) {
		super(name, attributes, secondaries, parent);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration<SecondaryTypeDeclaration>> attributes,
		Collection<String> secondaries) {
		super(name, attributes, secondaries);
	}

	public SecondaryTypeDeclaration(String name, Collection<AttributeDeclaration<SecondaryTypeDeclaration>> attributes,
		String parent) {
		super(name, attributes, parent);
	}

	public SecondaryTypeDeclaration(String name,
		Collection<AttributeDeclaration<SecondaryTypeDeclaration>> attributes) {
		super(name, attributes);
	}
}