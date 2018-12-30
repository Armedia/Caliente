package com.armedia.caliente.engine.importer.schema;

import java.util.Collection;

public interface SchemaService extends AutoCloseable {

	public Collection<String> getObjectTypeNames() throws SchemaServiceException;

	public TypeDeclaration getObjectTypeDeclaration(String typeName) throws SchemaServiceException;

	public Collection<String> getSecondaryTypeNames() throws SchemaServiceException;

	public TypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName)
		throws SchemaServiceException;

}