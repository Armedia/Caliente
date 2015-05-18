/**
 *
 */

package com.armedia.cmf.engine.converter;

import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.cmf.engine.converter.MappingManager.Mappable;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public enum IntermediateAttribute implements Mappable {
	// CMIS attributes
	OBJECT_ID(PropertyIds.OBJECT_ID, StoredDataType.ID),
	BASE_TYPE_ID(PropertyIds.BASE_TYPE_ID, StoredDataType.STRING),
	OBJECT_TYPE_ID(PropertyIds.OBJECT_TYPE_ID, StoredDataType.STRING),
	NAME(PropertyIds.NAME, StoredDataType.STRING),
	DESCRIPTION(PropertyIds.DESCRIPTION, StoredDataType.STRING),
	CONTENT_STREAM_MIME_TYPE(PropertyIds.CONTENT_STREAM_MIME_TYPE, StoredDataType.STRING),
	CONTENT_STREAM_LENGTH(PropertyIds.CONTENT_STREAM_LENGTH, StoredDataType.INTEGER),
	CONTENT_STREAM_HASH(PropertyIds.CONTENT_STREAM_HASH, StoredDataType.STRING),
	CREATED_BY(PropertyIds.CREATED_BY, StoredDataType.STRING),
	CREATION_DATE(PropertyIds.CREATION_DATE, StoredDataType.DATETIME),
	LAST_MODIFIED_BY(PropertyIds.LAST_MODIFIED_BY, StoredDataType.STRING),
	LAST_MODIFICATION_DATE(PropertyIds.LAST_MODIFICATION_DATE, StoredDataType.DATETIME),
	IS_IMMUTABLE(PropertyIds.IS_IMMUTABLE, StoredDataType.BOOLEAN),
	IS_LAST_VERSION(PropertyIds.IS_LATEST_VERSION, StoredDataType.BOOLEAN),
	PARENT_ID(PropertyIds.PARENT_ID, StoredDataType.ID, true),
	PATH(PropertyIds.PATH, StoredDataType.STRING, true),
	SOURCE_ID(PropertyIds.SOURCE_ID, StoredDataType.ID),
	TARGET_ID(PropertyIds.TARGET_ID, StoredDataType.ID),
	CHANGE_TOKEN(PropertyIds.CHANGE_TOKEN, StoredDataType.INTEGER),
	VERSION_LABEL(PropertyIds.VERSION_LABEL, StoredDataType.STRING),
	VERSION_SERIES_ID(PropertyIds.VERSION_SERIES_ID, StoredDataType.ID),
	VERSION_SERIES_CHECKED_OUT_BY(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, StoredDataType.STRING),
	CHECKIN_COMMENT(PropertyIds.CHECKIN_COMMENT, StoredDataType.STRING),

	// Non-CMIS attributes
	OWNER(StoredDataType.STRING),
	OWNER_PERMISSION(StoredDataType.STRING),
	GROUP(StoredDataType.STRING),
	GROUP_PERMISSION(StoredDataType.STRING),
	LAST_ACCESSED_BY(StoredDataType.STRING),
	LAST_ACCESS_DATE(StoredDataType.DATETIME),
	LOGIN_NAME(StoredDataType.STRING),
	LOGIN_REALM(StoredDataType.STRING),
	ACL_NAME(StoredDataType.STRING),
	DEFAULT_FOLDER(StoredDataType.STRING),
	VERSION_ANTECEDENT_ID(StoredDataType.ID),
	//
	;

	private final String name;
	public final StoredDataType type;
	public final boolean repeating;

	private IntermediateAttribute(String propertyId, StoredDataType type) {
		this(propertyId, type, false);
	}

	private IntermediateAttribute(StoredDataType type) {
		this(null, type, false);
	}

	private IntermediateAttribute(StoredDataType type, boolean repeating) {
		this(null, type, repeating);
	}

	private IntermediateAttribute(String propertyId, StoredDataType type, boolean repeating) {
		this.name = MappingManager.generateMapping(propertyId, name());
		this.type = type;
		this.repeating = repeating;
	}

	@Override
	public final String getMapping() {
		return this.name;
	}

	public final String encode() {
		return this.name;
	}

	private static volatile Map<String, IntermediateAttribute> MAPPINGS = null;

	private static void initMappings() {
		if (IntermediateAttribute.MAPPINGS == null) {
			synchronized (IntermediateAttribute.class) {
				if (IntermediateAttribute.MAPPINGS == null) {
					IntermediateAttribute.MAPPINGS = Tools.freezeMap(MappingManager.createMappings(
						IntermediateAttribute.class, IntermediateAttribute.values()));
				}
			}
		}
	}

	public static IntermediateAttribute decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		IntermediateAttribute.initMappings();
		IntermediateAttribute ret = IntermediateAttribute.MAPPINGS.get(name);
		if (ret == null) { throw new IllegalArgumentException(String.format(
			"Failed to decode [%s] into a valid intermediate attribute", name)); }
		return ret;
	}
}