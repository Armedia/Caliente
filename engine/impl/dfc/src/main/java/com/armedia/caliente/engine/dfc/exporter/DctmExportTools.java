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
package com.armedia.caliente.engine.dfc.exporter;

import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class DctmExportTools {

	public static ExportTarget getExportTarget(IDfPersistentObject source) throws DfException {
		if (source == null) { throw new IllegalArgumentException("Must provide an object to create a target for"); }
		final IDfId id = source.getObjectId();
		final DctmObjectType type = DctmObjectType.decodeType(source);
		if (type == null) { return null; }
		final String strId = id.getId();
		return ExportTarget.from(type.getStoredObjectType(), strId, strId);
	}

	public static ExportTarget getExportTarget(IDfTypedObject source) throws DfException {
		return DctmExportTools.getExportTarget(source, null);
	}

	public static ExportTarget getExportTarget(IDfTypedObject source, String idAttribute) throws DfException {
		return DctmExportTools.getExportTarget(source, idAttribute, null);
	}

	public static ExportTarget getExportTarget(IDfTypedObject source, String idAttribute, String typeAttribute)
		throws DfException {
		if (source == null) { throw new IllegalArgumentException("Must provide an object to create a target for"); }
		idAttribute = Tools.coalesce(idAttribute, DctmAttributes.R_OBJECT_ID);
		if (!source.hasAttr(idAttribute)) {
			throw new IllegalArgumentException(
				String.format("The ID attribute [%s] was not found in the given object", idAttribute));
		}
		final IDfId id = source.getId(idAttribute);

		// This is the best case scenario - we deduced the object's archetype from its ID,
		// so we don't need to analyze anything else.
		DctmObjectType dctmType = DctmObjectType.decodeType(id);
		if (dctmType == null) {
			// This is the worst case, slowest scenario where we have to actually analyze the object
			// type in play directly, either by getting the object type attribute or by analyzing
			// the object itself.
			typeAttribute = Tools.coalesce(typeAttribute, DctmAttributes.R_OBJECT_TYPE);
			if (source.hasAttr(typeAttribute)) {
				dctmType = DctmObjectType.decodeType(source.getSession(), source.getString(typeAttribute));
			} else {
				if (IDfPersistentObject.class.isInstance(source)) {
					dctmType = DctmObjectType.decodeType(IDfPersistentObject.class.cast(source));
				} else {
					final IDfSession session = source.getSession();
					String dql = "select t.name from dmi_object_type o, dm_type t where o.i_type = t.i_type and o.r_object_id = %s";
					try (DfcQuery query = new DfcQuery(session, String.format(dql, DfcUtils.quoteString(id.getId())),
						DfcQuery.Type.DF_EXECREAD_QUERY)) {
						if (query.hasNext()) {
							dctmType = DctmObjectType.decodeType(session, query.next().getString("name"));
						}
					}
				}
			}
		}
		CmfObject.Archetype objectType = null;
		if (dctmType != null) {
			objectType = dctmType.getStoredObjectType();
		}

		final String strId = id.getId();
		return ExportTarget.from(objectType, strId, strId);
	}

	public static ExportTarget getExportTarget(final IDfSession session, final IDfId id, String typeAttribute)
		throws DfException {
		if (id == null) { throw new IllegalArgumentException("Must provide an object ID to create a target for"); }

		// This is the best case scenario - we deduced the object's archetype from its ID,
		// so we don't need to analyze anything else.
		DctmObjectType dctmType = DctmObjectType.decodeType(id);
		if (dctmType == null) {
			// This is the worst case, slowest scenario where we have to actually analyze the object
			// type in play directly, either by getting the object type attribute or by analyzing
			// the object itself.
			typeAttribute = Tools.coalesce(typeAttribute, DctmAttributes.R_OBJECT_TYPE);
			String dql = "select t.name from dmi_object_type o, dm_type t where o.i_type = t.i_type and o.r_object_id = %s";
			try (DfcQuery query = new DfcQuery(session, String.format(dql, DfcUtils.quoteString(id.getId())),
				DfcQuery.Type.DF_EXECREAD_QUERY)) {
				if (query.hasNext()) {
					dctmType = DctmObjectType.decodeType(session, query.next().getString("name"));
				}
			}
		}
		CmfObject.Archetype objectType = null;
		if (dctmType != null) {
			objectType = dctmType.getStoredObjectType();
		}

		final String strId = id.getId();
		return ExportTarget.from(objectType, strId, strId);
	}
}