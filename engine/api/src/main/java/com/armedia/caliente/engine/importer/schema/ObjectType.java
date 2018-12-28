package com.armedia.caliente.engine.importer.schema;

import java.util.Map;
import java.util.Set;

import com.armedia.caliente.engine.importer.schema.decl.AttributeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.ObjectTypeDeclaration;

public class ObjectType extends SchemaMember<ObjectType, ObjectTypeDeclaration> {

	public static final ObjectType NULL = new ObjectType();

	private ObjectType() {
		super();
	}

	ObjectType(ObjectTypeDeclaration declaration, Set<String> ancestors, Set<String> secondaries,
		Map<String, AttributeDeclaration> attributes, String signature) {
		super(declaration, ancestors, secondaries, attributes, signature);
	}
}