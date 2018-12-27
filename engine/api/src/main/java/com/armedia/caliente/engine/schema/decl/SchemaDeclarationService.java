package com.armedia.caliente.engine.schema.decl;

import java.util.Collection;

public interface SchemaDeclarationService {

	public Collection<AttributeDeclaration> getAttributes(String typeName);

	public Collection<String> getTypeNames();

	public Collection<String> getSecondaryTypeNames();

	public TypeDeclaration getType(String typeName);

	public SecondaryTypeDeclaration getSecondaryType(String secondaryTypeName);
}