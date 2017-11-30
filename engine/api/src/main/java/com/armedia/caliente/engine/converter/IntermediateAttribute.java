/**
 *
 */

package com.armedia.caliente.engine.converter;

import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.caliente.engine.converter.MappingManager.Mappable;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public enum IntermediateAttribute implements Mappable, CmfEncodeableName {
	// CMIS attributes
	OBJECT_ID(PropertyIds.OBJECT_ID, CmfDataType.ID),
	BASE_TYPE_ID(PropertyIds.BASE_TYPE_ID, CmfDataType.STRING),
	OBJECT_TYPE_ID(PropertyIds.OBJECT_TYPE_ID, CmfDataType.STRING),
	NAME(PropertyIds.NAME, CmfDataType.STRING),
	DESCRIPTION(PropertyIds.DESCRIPTION, CmfDataType.STRING),
	CONTENT_STREAM_MIME_TYPE(PropertyIds.CONTENT_STREAM_MIME_TYPE, CmfDataType.STRING),
	CONTENT_STREAM_LENGTH(PropertyIds.CONTENT_STREAM_LENGTH, CmfDataType.INTEGER),
	CONTENT_STREAM_HASH(PropertyIds.CONTENT_STREAM_HASH, CmfDataType.STRING),
	CREATED_BY(PropertyIds.CREATED_BY, CmfDataType.STRING),
	CREATION_DATE(PropertyIds.CREATION_DATE, CmfDataType.DATETIME),
	LAST_MODIFIED_BY(PropertyIds.LAST_MODIFIED_BY, CmfDataType.STRING),
	LAST_MODIFICATION_DATE(PropertyIds.LAST_MODIFICATION_DATE, CmfDataType.DATETIME),
	IS_IMMUTABLE(PropertyIds.IS_IMMUTABLE, CmfDataType.BOOLEAN),
	IS_LATEST_VERSION(PropertyIds.IS_LATEST_VERSION, CmfDataType.BOOLEAN),
	PARENT_ID(PropertyIds.PARENT_ID, CmfDataType.ID, true),
	PATH(PropertyIds.PATH, CmfDataType.STRING, true),
	SOURCE_ID(PropertyIds.SOURCE_ID, CmfDataType.ID),
	TARGET_ID(PropertyIds.TARGET_ID, CmfDataType.ID),
	CHANGE_TOKEN(PropertyIds.CHANGE_TOKEN, CmfDataType.INTEGER),
	VERSION_LABEL(PropertyIds.VERSION_LABEL, CmfDataType.STRING),
	VERSION_SERIES_ID(PropertyIds.VERSION_SERIES_ID, CmfDataType.ID),
	VERSION_SERIES_CHECKED_OUT_BY(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, CmfDataType.STRING),
	CHECKIN_COMMENT(PropertyIds.CHECKIN_COMMENT, CmfDataType.STRING),
	SECONDARY_TYPE_IDS(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, CmfDataType.STRING),

	// Non-CMIS attributes
	SUPER_NAME(CmfDataType.STRING),
	OWNER(CmfDataType.STRING),
	OWNER_PERMISSION(CmfDataType.STRING),
	GROUP(CmfDataType.STRING),
	GROUP_PERMISSION(CmfDataType.STRING),
	LAST_ACCESSED_BY(CmfDataType.STRING),
	LAST_ACCESS_DATE(CmfDataType.DATETIME),
	LOGIN_NAME(CmfDataType.STRING),
	LOGIN_REALM(CmfDataType.STRING),
	OS_NAME(CmfDataType.STRING),
	OS_REALM(CmfDataType.STRING),
	EMAIL(CmfDataType.STRING),
	ADMINISTRATOR(CmfDataType.STRING),
	GROUP_TYPE(CmfDataType.STRING),
	ACL_NAME(CmfDataType.STRING),
	DEFAULT_FOLDER(CmfDataType.STRING),
	VERSION_ANTECEDENT_ID(CmfDataType.ID),
	USER_SOURCE(CmfDataType.STRING),
	GROUP_SOURCE(CmfDataType.STRING),
	UNFILED_FOLDER(CmfDataType.STRING),
	//
	;

	private static final Map<String, IntermediateAttribute> MAPPINGS = Tools
		.freezeMap(MappingManager.createMappings(IntermediateAttribute.class, IntermediateAttribute.values()));

	private final String name;
	public final CmfDataType type;
	public final boolean repeating;

	private IntermediateAttribute(String propertyId, CmfDataType type) {
		this(propertyId, type, false);
	}

	private IntermediateAttribute(CmfDataType type) {
		this(null, type, false);
	}

	private IntermediateAttribute(CmfDataType type, boolean repeating) {
		this(null, type, repeating);
	}

	private IntermediateAttribute(String propertyId, CmfDataType type, boolean repeating) {
		this.name = MappingManager.generateMapping(propertyId, name());
		this.type = type;
		this.repeating = repeating;
	}

	@Override
	public final String getMapping() {
		return this.name;
	}

	@Override
	public final String encode() {
		return this.name;
	}

	public static IntermediateAttribute decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		IntermediateAttribute ret = IntermediateAttribute.MAPPINGS.get(name);
		if (ret == null) { throw new IllegalArgumentException(
			String.format("Failed to decode [%s] into a valid intermediate attribute", name)); }
		return ret;
	}
}