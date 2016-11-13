package com.armedia.caliente.engine.documentum.exporter;

import com.armedia.caliente.engine.documentum.DctmAttributes;
import com.armedia.caliente.engine.documentum.DctmObjectType;
import com.armedia.caliente.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class DctmExportTools {

	public static ExportTarget getExportTarget(IDfPersistentObject source)
		throws DfException, UnsupportedDctmObjectTypeException {
		if (source == null) { throw new IllegalArgumentException("Must provide an object to create a target for"); }
		final IDfId id = source.getObjectId();
		final DctmObjectType type = DctmObjectType.decodeType(source);
		final String strId = id.getId();
		return new ExportTarget(type.getStoredObjectType(), strId, strId);
	}

	public static ExportTarget getExportTarget(IDfTypedObject source, String idAttribute, String typeAttribute)
		throws DfException, UnsupportedDctmObjectTypeException {
		if (source == null) { throw new IllegalArgumentException("Must provide an object to create a target for"); }
		idAttribute = Tools.coalesce(idAttribute, DctmAttributes.R_OBJECT_ID);
		if (!source.hasAttr(idAttribute)) { throw new IllegalArgumentException(
			String.format("The ID attribute [%s] was not found in the given object", idAttribute)); }
		final IDfId id = source.getId(idAttribute);
		final DctmObjectType dctmType = DctmObjectType.decodeType(id);
		final CmfType objectType;
		if (dctmType != null) {
			// This is the best case scenario - we deduced the object's archetype from its ID,
			// so we don't need to analyze anything else.
			objectType = dctmType.getStoredObjectType();
		} else {
			// This is the worst case, slowest scenario where we have to actually analyze the object
			// type in play directly, either by getting the object type attribute or by analyzing
			// the object itself.
			typeAttribute = Tools.coalesce(typeAttribute, DctmAttributes.R_OBJECT_TYPE);
			if (source.hasAttr(typeAttribute)) {
				objectType = DctmObjectType.decodeType(source.getSession(), source.getString(typeAttribute))
					.getStoredObjectType();
			} else {
				if (IDfPersistentObject.class.isInstance(source)) {
					objectType = DctmObjectType.decodeType(IDfPersistentObject.class.cast(source))
						.getStoredObjectType();
				} else {
					objectType = null;
				}
			}
		}

		final String strId = id.getId();
		return new ExportTarget(objectType, strId, strId);
	}
}