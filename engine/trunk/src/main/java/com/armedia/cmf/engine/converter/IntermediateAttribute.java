/**
 *
 */

package com.armedia.cmf.engine.converter;

import java.util.HashMap;
import java.util.Map;

import com.armedia.cmf.storage.StoredDataType;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public enum IntermediateAttribute {
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
	LOGIN_NAME(StoredDataType.STRING),
	LOGIN_REALM(StoredDataType.STRING),
	ACL_NAME(StoredDataType.STRING),
	DEFAULT_FOLDER(StoredDataType.STRING),
	VERSION_LABEL(StoredDataType.STRING),
	VERSION_TREE_ID(StoredDataType.ID),
	VERSION_PRIOR_ID(StoredDataType.ID),
	//
	;

	private final String name;
	public final StoredDataType type;
	public final boolean repeating;

	private IntermediateAttribute(StoredDataType type) {
		this(type, false);
	}

	private IntermediateAttribute(StoredDataType type, boolean repeating) {
		this.name = String.format(":%s", name().toLowerCase());
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
						mappings.put(a.name, a);
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