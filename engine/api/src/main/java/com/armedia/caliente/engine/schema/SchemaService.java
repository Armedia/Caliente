package com.armedia.caliente.engine.schema;

import java.util.Collection;

import com.armedia.caliente.engine.schema.decl.AttributeDeclaration;
import com.armedia.caliente.engine.schema.decl.SecondaryTypeDeclaration;
import com.armedia.caliente.engine.schema.decl.TypeDeclaration;

public interface SchemaService {

	public ObjectType constructType(String typeName, Collection<String> secondaries);

	public Collection<AttributeDeclaration> getAttributes(String typeName);

	public Collection<String> getTypeNames();

	public Collection<String> getSecondaryTypeNames();

	public TypeDeclaration getType(String typeName);

	public SecondaryTypeDeclaration getSecondaryType(String secondaryTypeName);

}