package com.armedia.caliente.engine.importer.schema.decl;

import java.util.Collection;

public interface SchemaDeclarationService {

	public Collection<String> getObjectTypeNames() throws SchemaDeclarationServiceException;

	public ObjectTypeDeclaration getObjectTypeDeclaration(String typeName) throws SchemaDeclarationServiceException;

	public Collection<String> getSecondaryTypeNames() throws SchemaDeclarationServiceException;

	public SecondaryTypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName)
		throws SchemaDeclarationServiceException;

}