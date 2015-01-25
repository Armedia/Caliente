/**
 *
 */

package com.armedia.cmf.engine.converter;

import com.armedia.cmf.storage.StoredDataType;

/**
 * @author diego
 *
 */
public enum IntermediateAttributes {
	//
	OBJECT_ID(StoredDataType.ID),
	OBJECT_CLASS(StoredDataType.STRING),
	OBJECT_TYPE(StoredDataType.STRING),
	OBJECT_NAME(StoredDataType.STRING),
	DESCRIPTION(StoredDataType.STRING),
	CONTENT_TYPE(StoredDataType.STRING),
	CONTENT_SIZE(StoredDataType.INTEGER),
	CONTENT_HASH(StoredDataType.STRING),
	OWNER(StoredDataType.STRING),
	OWNER_PERMISSION(StoredDataType.STRING),
	GROUP(StoredDataType.STRING),
	GROUP_PERMISSION(StoredDataType.STRING),
	CREATOR(StoredDataType.STRING),
	CREATE_DATE(StoredDataType.TIME),
	MODIFIER(StoredDataType.STRING),
	MODIFICATION_DATE(StoredDataType.TIME),
	ACCESSOR(StoredDataType.STRING),
	ACCESS_DATE(StoredDataType.TIME),
	PARENTS(StoredDataType.STRING, true),
	PATHS(StoredDataType.STRING, true),
	ACL_REALM(StoredDataType.STRING),
	ACL_NAME(StoredDataType.STRING),
	DEFAULT_FOLDER(StoredDataType.STRING),
	//
	;

	public final String name;
	public final StoredDataType type;
	public final boolean repeating;

	private IntermediateAttributes(StoredDataType type) {
		this(type, false);
	}

	private IntermediateAttributes(StoredDataType type, boolean repeating) {
		this.name = name().toLowerCase();
		this.type = type;
		this.repeating = repeating;
	}
}