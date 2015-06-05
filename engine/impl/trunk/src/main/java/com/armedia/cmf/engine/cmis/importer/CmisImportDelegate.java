package com.armedia.cmf.engine.cmis.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.cmis.CmisTranslator;
import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeMapper.Mapping;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public abstract class CmisImportDelegate<T>
	extends
	ImportDelegate<T, Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportDelegateFactory, CmisImportEngine> {

	protected CmisImportDelegate(CmisImportDelegateFactory factory, Class<T> objectClass,
		CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, objectClass, storedObject);
	}

	@Override
	protected ImportOutcome importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		return null;
	}

	protected Map<String, Object> prepareProperties(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfValueDecoderException {
		final Session session = ctx.getSession();
		final String typeName = this.cmfObject.getSubtype();

		Mapping m = ctx.getAttributeMapper().getTargetMapping(CmfType.TYPE, PropertyIds.NAME, typeName);
		final String finalTypeName;
		if (m == null) {
			BaseTypeId id = CmisTranslator.decodeObjectType(this.cmfObject.getType());
			if (id == null) { throw new ImportException(String.format(
				"Failed to identify the base type for %s of subtype [%s]  [%s](%s)", this.cmfObject.getType(),
				this.cmfObject.getSubtype(), this.cmfObject.getLabel(), this.cmfObject.getId())); }
			finalTypeName = id.value();
		} else {
			finalTypeName = m.getTargetValue();
		}

		final ObjectType type;
		try {
			type = session.getTypeDefinition(typeName);
		} catch (CmisObjectNotFoundException e) {
			throw new ImportException(String.format(
				"Failed to locate the type called [%s] (from source type [%s]) for %s [%s](%s)", finalTypeName,
				typeName, this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}

		Map<String, Object> properties = new HashMap<String, Object>();
		outer: for (PropertyDefinition<?> def : type.getPropertyDefinitions().values()) {
			CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(def.getId());
			if (att == null) {
				continue outer;
			}
			final CmfDataType targetType = CmisTranslator.decodePropertyType(def.getPropertyType());

			final Object value;
			if (!att.isRepeating() || (def.getCardinality() != Cardinality.MULTI)) {
				// Only copy the first one
				value = targetType.getValue(att.getValue());
			} else {
				// Copy ALL the values
				final int count = att.getValueCount();
				List<Object> l = new ArrayList<Object>(count);
				for (int i = 0; i < count; i++) {
					l.add(targetType.getValue(att.getValue(i)));
				}
				value = l;
			}

			properties.put(def.getId(), value);
		}

		return properties;
	}
}