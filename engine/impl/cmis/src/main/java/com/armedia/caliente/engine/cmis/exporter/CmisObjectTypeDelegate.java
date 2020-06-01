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
package com.armedia.caliente.engine.cmis.exporter;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;

import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class CmisObjectTypeDelegate extends CmisExportDelegate<ObjectType> {

	private static final Map<String, Function<PropertyDefinition<?>, Object>> READERS;
	static {
		Map<String, Function<PropertyDefinition<?>, Object>> readers = new HashMap<>();
		readers.put("id", PropertyDefinition::getId);
		readers.put("localNamespace", PropertyDefinition::getLocalNamespace);
		readers.put("localName", PropertyDefinition::getLocalName);
		readers.put("queryName", PropertyDefinition::getQueryName);
		readers.put("displayName", PropertyDefinition::getDisplayName);
		readers.put("description", PropertyDefinition::getDescription);
		readers.put("propertyType", PropertyDefinition::getPropertyType);
		readers.put("cardinality", PropertyDefinition::getCardinality);
		readers.put("updatability", PropertyDefinition::getUpdatability);
		readers.put("inherited", PropertyDefinition::isInherited);
		readers.put("required", PropertyDefinition::isRequired);
		readers.put("queryable", PropertyDefinition::isQueryable);
		readers.put("orderable", PropertyDefinition::isOrderable);
		readers.put("openChoice", PropertyDefinition::isOpenChoice);
		READERS = Tools.freezeMap(readers);
	}

	protected CmisObjectTypeDelegate(CmisExportDelegateFactory factory, Session session, ObjectType objectType)
		throws Exception {
		super(factory, session, ObjectType.class, objectType);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		// Don't marshal the details for base types?
		if (this.object.isBaseType()) { return true; }

		// Save the property definitions...
		for (PropertyDefinition<?> p : this.object.getPropertyDefinitions().values()) {
			// TODO: How to encode this information in an "engine-neutral" fashion?

			/*-
			"my:stringProperty":{
			    "id":"my:stringProperty",
			    "localNamespace":"local",
			    "localName":"my:stringProperty",
			    "queryName":"my:stringProperty",
			    "displayName":"My String Property",
			    "description":"This is a String.~,
			    "propertyType":"string",
			    "updatability":"readwrite",
			    "inherited":false,
			    "openChoice":false,
			    "required":false,
			    "cardinality":"single",
			    "queryable":true,
			    "orderable":true,
			}
			*/

			for (String name : CmisObjectTypeDelegate.READERS.keySet()) {
				Object o = CmisObjectTypeDelegate.READERS.get(name).apply(p);
				if (o == null) {
					continue;
				}

				if (o.getClass().isEnum()) {
					o = Enum.class.cast(o).name();
				} else {
					o = Tools.toString(o);
				}

				// TODO: Now, what?
			}

			// TODO: Handle these
			/*
			List<?> defaultValue = p.getDefaultValue();
			List<? extends Choice<?>> choices = p.getChoices();
			*/

		}
		return true;
	}

	protected int calculateDepth(ObjectType objectType, final Set<String> visited) throws Exception {
		if (objectType == null) {
			throw new IllegalArgumentException("Must provide a folder whose depth to calculate");
		}
		if (!visited.add(objectType.getId())) {
			throw new IllegalStateException(
				String.format("ObjectType [%s] was visited twice - visited set: %s", objectType.getId(), visited));
		}
		try {
			if (objectType.isBaseType()) { return 0; }
			return calculateDepth(objectType.getParentType(), visited) + 1;
		} finally {
			visited.remove(objectType.getId());
		}
	}

	@Override
	protected int calculateDependencyTier(Session session, ObjectType objectType) throws Exception {
		return calculateDepth(objectType, new LinkedHashSet<String>());
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyRequirements(marshalled, ctx);
		ObjectType objectType = this.object.getParentType();
		if (objectType != null) {
			ret.add(new CmisObjectTypeDelegate(this.factory, ctx.getSession(), objectType));
		}
		return ret;
	}

	@Override
	protected CmfObject.Archetype calculateType(Session session, ObjectType object) throws Exception {
		return CmfObject.Archetype.TYPE;
	}

	@Override
	protected String calculateLabel(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateObjectId(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateSearchKey(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateName(Session session, ObjectType object) throws Exception {
		return object.getId();
	}
}