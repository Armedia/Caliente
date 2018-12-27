package com.armedia.caliente.engine.importer.schema.decl;

import java.util.Collection;

public interface SchemaDeclarationService {

	public Collection<String> getTypeNames() throws SchemaDeclarationException;

	public TypeDeclaration getType(String typeName) throws SchemaDeclarationException;

	public Collection<String> getSecondaryTypeNames() throws SchemaDeclarationException;

	public SecondaryTypeDeclaration getSecondaryType(String secondaryTypeName) throws SchemaDeclarationException;

}