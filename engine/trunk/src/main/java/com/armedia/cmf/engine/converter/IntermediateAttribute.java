/**
 *
 */

package com.armedia.cmf.engine.converter;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.cmf.storage.StoredDataType;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public enum IntermediateAttribute {
	// CMIS attributes
	OBJECT_ID(PropertyIds.OBJECT_ID, StoredDataType.ID),
	OBJECT_CLASS(PropertyIds.BASE_TYPE_ID, StoredDataType.STRING),
	OBJECT_TYPE(PropertyIds.OBJECT_TYPE_ID, StoredDataType.STRING),
	OBJECT_NAME(PropertyIds.NAME, StoredDataType.STRING),
	DESCRIPTION(PropertyIds.DESCRIPTION, StoredDataType.STRING),
	CONTENT_TYPE(PropertyIds.CONTENT_STREAM_MIME_TYPE, StoredDataType.STRING),
	CONTENT_SIZE(PropertyIds.CONTENT_STREAM_LENGTH, StoredDataType.INTEGER),
	CONTENT_HASH(PropertyIds.CONTENT_STREAM_HASH, StoredDataType.STRING),
	CREATOR(PropertyIds.CREATED_BY, StoredDataType.STRING),
	CREATE_DATE(PropertyIds.CREATION_DATE, StoredDataType.TIME),
	MODIFIER(PropertyIds.LAST_MODIFIED_BY, StoredDataType.STRING),
	MODIFICATION_DATE(PropertyIds.LAST_MODIFICATION_DATE, StoredDataType.TIME),
	IS_IMMUTABLE(PropertyIds.IS_IMMUTABLE, StoredDataType.BOOLEAN),
	IS_LAST_VERSION(PropertyIds.IS_LATEST_VERSION, StoredDataType.BOOLEAN),
	PARENTS(PropertyIds.PARENT_ID, StoredDataType.ID, true),
	PATHS(PropertyIds.PATH, StoredDataType.STRING, true),
	SOURCE_ID(PropertyIds.SOURCE_ID, StoredDataType.ID),
	TARGET_ID(PropertyIds.TARGET_ID, StoredDataType.ID),
	VERSION_TOKEN(PropertyIds.CHANGE_TOKEN, StoredDataType.INTEGER),
	VERSION_LABEL(PropertyIds.VERSION_LABEL, StoredDataType.STRING),
	VERSION_TREE_ID(PropertyIds.VERSION_SERIES_ID, StoredDataType.ID),
	VERSION_LOCK_OWNER(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, StoredDataType.STRING),

	// Non-CMIS attributes
	OWNER(StoredDataType.STRING),
	OWNER_PERMISSION(StoredDataType.STRING),
	GROUP(StoredDataType.STRING),
	GROUP_PERMISSION(StoredDataType.STRING),
	ACCESSOR(StoredDataType.STRING),
	ACCESS_DATE(StoredDataType.TIME),
	LOGIN_NAME(StoredDataType.STRING),
	LOGIN_REALM(StoredDataType.STRING),
	ACL_NAME(StoredDataType.STRING),
	DEFAULT_FOLDER(StoredDataType.STRING),
	VERSION_PRIOR_ID(StoredDataType.ID),
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
		this.name = (propertyId != null ? propertyId : String.format(":%s", name().toLowerCase()));
		this.type = type;
		this.repeating = repeating;
	}

	public final String encode() {
		return this.name;
	}

	private static volatile Map<String, IntermediateAttribute> MAPPINGS = null;

	private static void initMappings() {
		if (IntermediateAttribute.MAPPINGS == null) {
			synchronized (IntermediateAttribute.class) {
				if (IntermediateAttribute.MAPPINGS == null) {
					Map<String, IntermediateAttribute> mappings = new HashMap<String, IntermediateAttribute>();
					for (IntermediateAttribute a : IntermediateAttribute.values()) {
						IntermediateAttribute b = mappings.put(a.name, a);
						if (b != null) { throw new IllegalStateException(String.format(
							"Both intermediate attributes %s and %s resolve the same mapping name (%s|%s)", a, b,
							a.name, b.name)); }
					}
					IntermediateAttribute.MAPPINGS = Tools.freezeMap(mappings);
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