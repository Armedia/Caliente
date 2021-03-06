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
public enum IntermediateProperty implements Supplier<String>, CmfEncodeableName {
	// CMIS-inspired properties
	PATH(PropertyIds.PATH, CmfValue.Type.STRING),
	PARENT_ID(PropertyIds.PARENT_ID, CmfValue.Type.ID),
	CONTENT_STREAM_ID(PropertyIds.CONTENT_STREAM_ID, CmfValue.Type.STRING),

	// Non-CMIS properties
	FULL_PATH(CmfValue.Type.STRING),
	PARENT_TREE_IDS(CmfValue.Type.STRING),
	LATEST_PARENT_TREE_IDS(CmfValue.Type.STRING),
	ACL_ID(CmfValue.Type.ID),
	ACL_INHERITANCE(CmfValue.Type.BOOLEAN),
	ACL_OWNER(CmfValue.Type.STRING),
	ACL_OBJECT_ID(CmfValue.Type.STRING),
	ACL_ACCESSOR_NAME(CmfValue.Type.STRING),
	ACL_ACCESSOR_TYPE(CmfValue.Type.STRING),
	ACL_ACCESSOR_ACTIONS(CmfValue.Type.STRING),
	ACL_PERMISSION_NAME(CmfValue.Type.STRING),
	VERSION_TREE_ROOT(CmfValue.Type.BOOLEAN),
	VERSION_COUNT(CmfValue.Type.INTEGER),
	VERSION_HEAD_INDEX(CmfValue.Type.INTEGER),
	VERSION_INDEX(CmfValue.Type.INTEGER),
	VERSION_PATCHES(CmfValue.Type.STRING),
	IS_NEWEST_VERSION(CmfValue.Type.BOOLEAN),
	IS_UNFILED(CmfValue.Type.BOOLEAN),
	PATCH_ANTECEDENT(CmfValue.Type.STRING),
	USERS_WITH_DEFAULT_FOLDER(CmfValue.Type.STRING),
	USERS_DEFAULT_FOLDER_PATHS(CmfValue.Type.STRING),
	USERS_WITH_DEFAULT_ACL(CmfValue.Type.STRING),
	USERS_WITH_DEFAULT_GROUP(CmfValue.Type.STRING),
	GROUPS_WITH_DEFAULT_FOLDER(CmfValue.Type.STRING),
	ORIG_ATTR_NAME(CmfValue.Type.STRING),
	MAPPED_ATTR_NAME(CmfValue.Type.STRING),
	VDOC_HISTORY(CmfValue.Type.BOOLEAN),
	VDOC_MEMBER(CmfValue.Type.STRING),
	DEFAULT_ACL(CmfValue.Type.STRING),
	DEFAULT_ASPECTS(CmfValue.Type.STRING),
	DEFAULT_STORAGE(CmfValue.Type.STRING),
	IS_REFERENCE(CmfValue.Type.BOOLEAN),
	REF_TARGET(CmfValue.Type.STRING),
	REF_VERSION(CmfValue.Type.STRING),
	HEAD_NAME(CmfValue.Type.STRING),
	FIXED_NAME(CmfValue.Type.STRING),
	FIXED_PATH(CmfValue.Type.STRING),
	PRESERVED_NAME(CmfValue.Type.STRING),
	PROPERTY_DEFINITIONS(CmfValue.Type.STRING),
	TYPE_DEFINITION_XML(CmfValue.Type.STRING),
	TYPE_DEFINITION_JSON(CmfValue.Type.STRING),
	//
	;

	private final String name;
	public final CmfValue.Type type;
	public final boolean repeating;

	private IntermediateProperty(CmfValue.Type type) {
		this(null, type, false);
	}

	private IntermediateProperty(String propertyId, CmfValue.Type type) {
		this(propertyId, type, false);
	}

	private IntermediateProperty(CmfValue.Type type, boolean repeating) {
		this(null, type, repeating);
	}

	private IntermediateProperty(String propertyId, CmfValue.Type type, boolean repeating) {
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

	private static final Map<String, IntermediateProperty> MAPPINGS;

	static {
		MAPPINGS = Tools
			.freezeMap(MappingManager.createMappings(IntermediateProperty.class, IntermediateProperty.values()));
	}

	public static IntermediateProperty decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		IntermediateProperty ret = IntermediateProperty.MAPPINGS.get(name);
		if (ret == null) {
			throw new IllegalArgumentException(
				String.format("Failed to decode [%s] into a valid intermediate property", name));
		}
		return ret;
	}
}