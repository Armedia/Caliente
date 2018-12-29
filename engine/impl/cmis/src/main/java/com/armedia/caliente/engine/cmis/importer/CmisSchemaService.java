package com.armedia.caliente.engine.cmis.importer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.armedia.caliente.engine.cmis.CmisTranslator;
import com.armedia.caliente.engine.importer.schema.decl.AttributeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.SchemaDeclarationServiceException;
import com.armedia.caliente.engine.importer.schema.decl.SchemaService;
import com.armedia.caliente.engine.importer.schema.decl.TypeDeclaration;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

public class CmisSchemaService implements SchemaService {

	private final Session session;

	public CmisSchemaService(Session session) {
		this.session = session;
	}

	protected void harvestChildTypes(Tree<ObjectType> node, final Set<String> types) {
		ObjectType type = node.getItem();
		types.add(type.getId());
		for (Tree<ObjectType> child : node.getChildren()) {
			harvestChildTypes(child, types);
		}
	}

	private Collection<String> getTypeNames(boolean secondary) throws SchemaDeclarationServiceException {
		Set<String> objectTypes = new TreeSet<>();
		for (Tree<ObjectType> tree : this.session.getTypeDescendants(null, -1, false)) {
			// The first level are the base types...consider them for now, eventually we may just
			// ignore them
			ObjectType node = tree.getItem();
			// Ignore secondary types
			if (secondary != Tools.equals(BaseTypeId.CMIS_SECONDARY, node.getId())) {
				continue;
			}
			harvestChildTypes(tree, objectTypes);
		}
		return objectTypes;
	}

	private TypeDeclaration getTypeDeclaration(String typeName, boolean secondary)
		throws SchemaDeclarationServiceException {

		final ObjectType type;
		try {
			type = this.session.getTypeDefinition(typeName);
		} catch (CmisObjectNotFoundException e) {
			return null;
		}

		ObjectType parent = type;
		while (parent.getParentType() != null) {
			parent = type.getParentType();
		}
		// Check to see if the root base type is the secondary type
		if (secondary != Tools.equals(BaseTypeId.CMIS_SECONDARY, parent.getId())) { return null; }
		Map<String, PropertyDefinition<?>> properties = type.getPropertyDefinitions();
		Map<String, AttributeDeclaration> attributes = new TreeMap<>();
		for (String name : properties.keySet()) {
			PropertyDefinition<?> definition = properties.get(name);
			if (definition.isInherited()) {
				// Don't include inherited properties
				continue;
			}
			CmfDataType dataType = CmisTranslator.decodePropertyType(definition.getPropertyType());
			attributes.put(name, new AttributeDeclaration(name, dataType, definition.isRequired(),
				definition.getCardinality() == Cardinality.MULTI));
		}
		// TODO: CMIS doesn't yet support the idea of secondaries attached to a type definition
		Collection<String> secondaries = Collections.emptyList();
		return new TypeDeclaration(type.getId(), attributes.values(), secondaries, type.getParentTypeId());
	}

	@Override
	public Collection<String> getObjectTypeNames() throws SchemaDeclarationServiceException {
		return getTypeNames(false);
	}

	@Override
	public TypeDeclaration getObjectTypeDeclaration(String typeName) throws SchemaDeclarationServiceException {
		return getTypeDeclaration(typeName, false);
	}

	@Override
	public Collection<String> getSecondaryTypeNames() throws SchemaDeclarationServiceException {
		return getTypeNames(true);
	}

	@Override
	public TypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName)
		throws SchemaDeclarationServiceException {
		return getTypeDeclaration(secondaryTypeName, true);
	}

	@Override
	public void close() throws Exception {
	}
}