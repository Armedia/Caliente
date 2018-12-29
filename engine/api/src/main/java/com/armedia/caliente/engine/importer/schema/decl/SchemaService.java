package com.armedia.caliente.engine.importer.schema.decl;

import java.util.Collection;

public interface SchemaService extends AutoCloseable {

	public Collection<String> getObjectTypeNames() throws SchemaDeclarationServiceException;

	public TypeDeclaration getObjectTypeDeclaration(String typeName) throws SchemaDeclarationServiceException;

	public Collection<String> getSecondaryTypeNames() throws SchemaDeclarationServiceException;

	public TypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName)
		throws SchemaDeclarationServiceException;

}