package com.armedia.caliente.engine.importer.schema.decl;

import java.util.Collection;

public interface SchemaDeclarationService {

	public Collection<String> getTypeNames() throws SchemaServiceException;

	public TypeDeclaration getTypeDeclaration(String typeName) throws SchemaServiceException;

	public Collection<String> getSecondaryTypeNames() throws SchemaServiceException;

	public SecondaryTypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName) throws SchemaServiceException;

}