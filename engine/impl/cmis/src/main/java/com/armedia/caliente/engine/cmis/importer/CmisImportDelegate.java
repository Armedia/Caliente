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
import com.armedia.caliente.store.CmfValueType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfArchetype;
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
		final String typeName = this.cmfObject.getSubtype();

		Mapping m = ctx.getValueMapper().getTargetMapping(CmfArchetype.TYPE, PropertyIds.NAME, typeName);
		final String finalTypeName;
		if (m == null) {
			BaseTypeId id = CmisTranslator.decodeObjectType(this.cmfObject.getType());
			if (id == null) { throw new ImportException(
				String.format("Failed to identify the base type for %s of subtype [%s]",
					this.cmfObject.getDescription(), this.cmfObject.getSubtype())); }
			finalTypeName = id.value();
		} else {
			finalTypeName = m.getTargetValue();
		}

		final ObjectType type;
		try {
			type = session.getTypeDefinition(finalTypeName);
		} catch (CmisObjectNotFoundException e) {
			throw new ImportException(
				String.format("Failed to locate the type called [%s] (from source type [%s]) for %s", finalTypeName,
					typeName, this.cmfObject.getDescription()),
				e);
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

			final CmfValueType targetType = CmisTranslator.decodePropertyType(def.getPropertyType());

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