/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
/**
 *
 */

package com.armedia.caliente.engine.converter;

import java.util.Map;
import java.util.function.Supplier;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

/**
 *
 *
 */
public enum IntermediateAttribute implements Supplier<String>, CmfEncodeableName {
	// CMIS attributes
	OBJECT_ID(PropertyIds.OBJECT_ID, CmfValue.Type.ID),
	BASE_TYPE_ID(PropertyIds.BASE_TYPE_ID, CmfValue.Type.STRING),
	OBJECT_TYPE_ID(PropertyIds.OBJECT_TYPE_ID, CmfValue.Type.STRING),
	NAME(PropertyIds.NAME, CmfValue.Type.STRING),
	DESCRIPTION(PropertyIds.DESCRIPTION, CmfValue.Type.STRING),
	CONTENT_STREAM_MIME_TYPE(PropertyIds.CONTENT_STREAM_MIME_TYPE, CmfValue.Type.STRING),
	CONTENT_STREAM_LENGTH(PropertyIds.CONTENT_STREAM_LENGTH, CmfValue.Type.INTEGER),
	CONTENT_STREAM_HASH(PropertyIds.CONTENT_STREAM_HASH, CmfValue.Type.STRING),
	CREATED_BY(PropertyIds.CREATED_BY, CmfValue.Type.STRING),
	CREATION_DATE(PropertyIds.CREATION_DATE, CmfValue.Type.DATETIME),
	LAST_MODIFIED_BY(PropertyIds.LAST_MODIFIED_BY, CmfValue.Type.STRING),
	LAST_MODIFICATION_DATE(PropertyIds.LAST_MODIFICATION_DATE, CmfValue.Type.DATETIME),
	IS_IMMUTABLE(PropertyIds.IS_IMMUTABLE, CmfValue.Type.BOOLEAN),
	IS_LATEST_VERSION(PropertyIds.IS_LATEST_VERSION, CmfValue.Type.BOOLEAN),
	PARENT_ID(PropertyIds.PARENT_ID, CmfValue.Type.ID, true),
	PATH(PropertyIds.PATH, CmfValue.Type.STRING, true),
	SOURCE_ID(PropertyIds.SOURCE_ID, CmfValue.Type.ID),
	TARGET_ID(PropertyIds.TARGET_ID, CmfValue.Type.ID),
	CHANGE_TOKEN(PropertyIds.CHANGE_TOKEN, CmfValue.Type.INTEGER),
	VERSION_LABEL(PropertyIds.VERSION_LABEL, CmfValue.Type.STRING),
	VERSION_SERIES_ID(PropertyIds.VERSION_SERIES_ID, CmfValue.Type.ID),
	VERSION_SERIES_CHECKED_OUT_BY(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, CmfValue.Type.STRING),
	CHECKIN_COMMENT(PropertyIds.CHECKIN_COMMENT, CmfValue.Type.STRING),
	SECONDARY_TYPE_IDS(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, CmfValue.Type.STRING),

	// Non-CMIS attributes
	SUPER_NAME(CmfValue.Type.STRING),
	OWNER(CmfValue.Type.STRING),
	OWNER_PERMISSION(CmfValue.Type.STRING),
	GROUP(CmfValue.Type.STRING),
	GROUP_PERMISSION(CmfValue.Type.STRING),
	LAST_ACCESSED_BY(CmfValue.Type.STRING),
	LAST_ACCESS_DATE(CmfValue.Type.DATETIME),
	LOGIN_NAME(CmfValue.Type.STRING),
	LOGIN_REALM(CmfValue.Type.STRING),
	OS_NAME(CmfValue.Type.STRING),
	OS_REALM(CmfValue.Type.STRING),
	EMAIL(CmfValue.Type.STRING),
	ADMINISTRATOR(CmfValue.Type.STRING),
	GROUP_TYPE(CmfValue.Type.STRING),
	ACL_NAME(CmfValue.Type.STRING),
	DEFAULT_FOLDER(CmfValue.Type.STRING),
	VERSION_ANTECEDENT_ID(CmfValue.Type.ID),
	USER_SOURCE(CmfValue.Type.STRING),
	GROUP_SOURCE(CmfValue.Type.STRING),
	UNFILED_FOLDER(CmfValue.Type.STRING),
	IS_REFERENCE(CmfValue.Type.BOOLEAN),
	//
	;

	private static final Map<String, IntermediateAttribute> MAPPINGS = Tools
		.freezeMap(MappingManager.createMappings(IntermediateAttribute.class, IntermediateAttribute.values()));

	private final String name;
	public final CmfValue.Type type;
	public final boolean repeating;

	private IntermediateAttribute(String propertyId, CmfValue.Type type) {
		this(propertyId, type, false);
	}

	private IntermediateAttribute(CmfValue.Type type) {
		this(null, type, false);
	}

	private IntermediateAttribute(CmfValue.Type type, boolean repeating) {
		this(null, type, repeating);
	}

	private IntermediateAttribute(String propertyId, CmfValue.Type type, boolean repeating) {
		this.name = MappingManager.generateMapping(propertyId, name());
		this.type = type;
		this.repeating = repeating;
	}

	@Override
	public final String get() {
		return this.name;
	}

	@Override
	public final String encode() {
		return this.name;
	}

	public static IntermediateAttribute decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		IntermediateAttribute ret = IntermediateAttribute.MAPPINGS.get(name);
		if (ret == null) {
			throw new IllegalArgumentException(
				String.format("Failed to decode [%s] into a valid intermediate attribute", name));
		}
		return ret;
	}
}