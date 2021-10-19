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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.cmis.CmisTranslator;
import com.armedia.caliente.engine.importer.ImportDelegate;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper.Mapping;

public abstract class CmisImportDelegate<T> extends
	ImportDelegate<T, Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportDelegateFactory, CmisImportEngine> {

	protected CmisImportDelegate(CmisImportDelegateFactory factory, Class<T> objectClass,
		CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, objectClass, storedObject);
	}

	/*
	@Override
	protected Collection<ImportOutcome> importObject(TypeDescriptor targetType,
		CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException {
		return null;
	}
	*/

	protected boolean skipAttribute(String name) {
		return false;
	}

	protected Map<String, Object> prepareProperties(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException {
		final Session session = ctx.getSession();
		final String typeName;

		boolean mapped = true;
		CmfAttribute<CmfValue> cmisObjectTypeAtt = this.cmfObject.getAttribute(PropertyIds.OBJECT_TYPE_ID);
		if ((cmisObjectTypeAtt != null) && cmisObjectTypeAtt.hasValues()) {
			CmfValue v = cmisObjectTypeAtt.getValue();
			if (!v.isNull()) {
				typeName = v.asString();
			} else {
				typeName = this.cmfObject.getSubtype();
			}
		} else {
			typeName = this.cmfObject.getSubtype();
		}

		Mapping m = ctx.getValueMapper().getTargetMapping(CmfObject.Archetype.TYPE, PropertyIds.NAME, typeName);
		final String finalTypeName;
		if (m == null) {
			mapped = false;
			// No existing mapping, so do the lookup
			String resolvedType = null;
			try {
				ObjectType finalType = ctx.getSession().getTypeDefinition(typeName);
				resolvedType = finalType.getId();
			} catch (CmisObjectNotFoundException e) {
				// No type... so fall back
				BaseTypeId id = CmisTranslator.decodeObjectType(this.cmfObject.getType());
				if (id == null) {
					throw new ImportException(String.format("Failed to identify the base type for %s of subtype [%s]",
						this.cmfObject.getDescription(), this.cmfObject.getSubtype()));
				}
				resolvedType = id.value();
			} finally {
				finalTypeName = resolvedType;
			}
		} else {
			finalTypeName = m.getTargetValue();
		}

		// This is just for redundancy
		final ObjectType type;
		try {
			type = session.getTypeDefinition(finalTypeName);
		} catch (CmisObjectNotFoundException e) {
			throw new ImportException(
				String.format("Failed to locate the type called [%s] (from source type [%s]) for %s", finalTypeName,
					typeName, this.cmfObject.getDescription()),
				e);
		}

		// Store the mapping if needed
		if (!mapped) {
			ctx.getValueMapper().setMapping(CmfObject.Archetype.TYPE, PropertyIds.NAME, typeName, type.getId());
		}

		Map<String, Object> properties = new HashMap<>();
		properties.put(PropertyIds.OBJECT_TYPE_ID, finalTypeName);
		for (PropertyDefinition<?> def : type.getPropertyDefinitions().values()) {
			CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(def.getId());
			if ((att == null) || !att.hasValues()) {
				// No such attribute or no values...move along!
				continue;
			}
			if (skipAttribute(def.getId())) {
				// Attribute not of interest (at least, not yet)...move along!
				continue;
			}

			final CmfValue.Type targetType = CmisTranslator.decodePropertyType(def.getPropertyType());

			Object value = null;
			switch (def.getCardinality()) {
				case MULTI:
					// Copy ALL the values
					final int count = att.getValueCount();
					List<Object> l = new ArrayList<>(count);
					for (int i = 0; i < count; i++) {
						CmfValue v = att.getValue(i);
						if (v.isNull()) {
							// Don't add null-values...right?
							l.add(targetType.getValue(v));
						}
					}
					value = l;
					break;

				case SINGLE:
					// Only copy the first one
					CmfValue v = att.getValue();
					if (!v.isNull()) {
						value = targetType.getValue(v);
					}
					break;
			}

			// Only put the property in if it hasn't already been put in...
			if ((value != null) && !properties.containsKey(def.getId())) {
				properties.put(def.getId(), value);
			}
		}

		return properties;
	}
}