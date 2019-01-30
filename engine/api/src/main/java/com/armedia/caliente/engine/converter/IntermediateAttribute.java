/**
 *
 */

package com.armedia.caliente.engine.converter;

import java.util.Map;
import java.util.function.Supplier;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.caliente.store.CmfValueType;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public enum IntermediateAttribute implements Supplier<String>, CmfEncodeableName {
	// CMIS attributes
	OBJECT_ID(PropertyIds.OBJECT_ID, CmfValueType.ID),
	BASE_TYPE_ID(PropertyIds.BASE_TYPE_ID, CmfValueType.STRING),
	OBJECT_TYPE_ID(PropertyIds.OBJECT_TYPE_ID, CmfValueType.STRING),
	NAME(PropertyIds.NAME, CmfValueType.STRING),
	DESCRIPTION(PropertyIds.DESCRIPTION, CmfValueType.STRING),
	CONTENT_STREAM_MIME_TYPE(PropertyIds.CONTENT_STREAM_MIME_TYPE, CmfValueType.STRING),
	CONTENT_STREAM_LENGTH(PropertyIds.CONTENT_STREAM_LENGTH, CmfValueType.INTEGER),
	CONTENT_STREAM_HASH(PropertyIds.CONTENT_STREAM_HASH, CmfValueType.STRING),
	CREATED_BY(PropertyIds.CREATED_BY, CmfValueType.STRING),
	CREATION_DATE(PropertyIds.CREATION_DATE, CmfValueType.DATETIME),
	LAST_MODIFIED_BY(PropertyIds.LAST_MODIFIED_BY, CmfValueType.STRING),
	LAST_MODIFICATION_DATE(PropertyIds.LAST_MODIFICATION_DATE, CmfValueType.DATETIME),
	IS_IMMUTABLE(PropertyIds.IS_IMMUTABLE, CmfValueType.BOOLEAN),
	IS_LATEST_VERSION(PropertyIds.IS_LATEST_VERSION, CmfValueType.BOOLEAN),
	PARENT_ID(PropertyIds.PARENT_ID, CmfValueType.ID, true),
	PATH(PropertyIds.PATH, CmfValueType.STRING, true),
	SOURCE_ID(PropertyIds.SOURCE_ID, CmfValueType.ID),
	TARGET_ID(PropertyIds.TARGET_ID, CmfValueType.ID),
	CHANGE_TOKEN(PropertyIds.CHANGE_TOKEN, CmfValueType.INTEGER),
	VERSION_LABEL(PropertyIds.VERSION_LABEL, CmfValueType.STRING),
	VERSION_SERIES_ID(PropertyIds.VERSION_SERIES_ID, CmfValueType.ID),
	VERSION_SERIES_CHECKED_OUT_BY(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, CmfValueType.STRING),
	CHECKIN_COMMENT(PropertyIds.CHECKIN_COMMENT, CmfValueType.STRING),
	SECONDARY_TYPE_IDS(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, CmfValueType.STRING),

	// Non-CMIS attributes
	SUPER_NAME(CmfValueType.STRING),
	OWNER(CmfValueType.STRING),
	OWNER_PERMISSION(CmfValueType.STRING),
	GROUP(CmfValueType.STRING),
	GROUP_PERMISSION(CmfValueType.STRING),
	LAST_ACCESSED_BY(CmfValueType.STRING),
	LAST_ACCESS_DATE(CmfValueType.DATETIME),
	LOGIN_NAME(CmfValueType.STRING),
	LOGIN_REALM(CmfValueType.STRING),
	OS_NAME(CmfValueType.STRING),
	OS_REALM(CmfValueType.STRING),
	EMAIL(CmfValueType.STRING),
	ADMINISTRATOR(CmfValueType.STRING),
	GROUP_TYPE(CmfValueType.STRING),
	ACL_NAME(CmfValueType.STRING),
	DEFAULT_FOLDER(CmfValueType.STRING),
	VERSION_ANTECEDENT_ID(CmfValueType.ID),
	USER_SOURCE(CmfValueType.STRING),
	GROUP_SOURCE(CmfValueType.STRING),
	UNFILED_FOLDER(CmfValueType.STRING),
	IS_REFERENCE(CmfValueType.BOOLEAN),
	//
	;

	private static final Map<String, IntermediateAttribute> MAPPINGS = Tools
		.freezeMap(MappingManager.createMappings(IntermediateAttribute.class, IntermediateAttribute.values()));

	private final String name;
	public final CmfValueType type;
	public final boolean repeating;

	private IntermediateAttribute(String propertyId, CmfValueType type) {
		this(propertyId, type, false);
	}

	private IntermediateAttribute(CmfValueType type) {
		this(null, type, false);
	}

	private IntermediateAttribute(CmfValueType type, boolean repeating) {
		this(null, type, repeating);
	}

	private IntermediateAttribute(String propertyId, CmfValueType type, boolean repeating) {
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