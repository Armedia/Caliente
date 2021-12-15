/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.cmis.importer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
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
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.AttributeDeclaration;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.TypeDeclaration;
import com.armedia.caliente.store.CmfValue;

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

	private Collection<String> getTypeNames(boolean secondary) throws SchemaServiceException {
		Set<String> objectTypes = new TreeSet<>();
		for (Tree<ObjectType> tree : this.session.getTypeDescendants(null, -1, false)) {
			// The first level are the base types...consider them for now, eventually we may just
			// ignore them
			ObjectType node = tree.getItem();
			// Ignore secondary types
			if (secondary != Objects.equals(BaseTypeId.CMIS_SECONDARY.value(), node.getId())) {
				continue;
			}
			harvestChildTypes(tree, objectTypes);
		}
		return objectTypes;
	}

	private TypeDeclaration getTypeDeclaration(String typeName, boolean secondary) throws SchemaServiceException {

		final ObjectType type;
		try {
			type = this.session.getTypeDefinition(typeName);
		} catch (CmisObjectNotFoundException e) {
			return null;
		}

		ObjectType baseType = type.getBaseType();
		if (baseType == null) {
			baseType = type;
		}

		// Check to see if the root base type is the secondary type
		if (secondary != Objects.equals(BaseTypeId.CMIS_SECONDARY.value(), baseType.getId())) { return null; }
		Map<String, PropertyDefinition<?>> properties = type.getPropertyDefinitions();
		Map<String, AttributeDeclaration> attributes = new TreeMap<>();
		for (String name : properties.keySet()) {
			PropertyDefinition<?> definition = properties.get(name);
			if (definition.isInherited()) {
				// Don't include inherited properties
				continue;
			}
			CmfValue.Type dataType = CmisTranslator.decodePropertyType(definition.getPropertyType());
			attributes.put(name, new AttributeDeclaration(name, dataType, definition.isRequired(),
				definition.getCardinality() == Cardinality.MULTI));
		}
		// TODO: CMIS doesn't yet support the idea of secondaries attached to a type definition
		Collection<String> secondaries = Collections.emptyList();
		return new TypeDeclaration(type.getId(), attributes.values(), secondaries, type.getParentTypeId());
	}

	@Override
	public Collection<String> getObjectTypeNames() throws SchemaServiceException {
		return getTypeNames(false);
	}

	@Override
	public TypeDeclaration getObjectTypeDeclaration(String typeName) throws SchemaServiceException {
		return getTypeDeclaration(typeName, false);
	}

	@Override
	public Collection<String> getSecondaryTypeNames() throws SchemaServiceException {
		return getTypeNames(true);
	}

	@Override
	public TypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName) throws SchemaServiceException {
		return getTypeDeclaration(secondaryTypeName, true);
	}

	@Override
	public void close() {
	}
}