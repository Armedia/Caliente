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
public enum IntermediateProperty {
	//
	USERS_WITH_DEFAULT_GROUP(StoredDataType.ID),
	TARGET_PATHS(StoredDataType.STRING),
	TARGET_PARENTS(StoredDataType.STRING),
	USERS_WITH_DEFAULT_FOLDER(StoredDataType.STRING),
	USERS_WITH_DEFAULT_FOLDER_PATHS(StoredDataType.STRING),
	//
	;

	private final String name;
	public final StoredDataType type;
	public final boolean repeating;

	private IntermediateProperty(StoredDataType type) {
		this(type, false);
	}

	private IntermediateProperty(StoredDataType type, boolean repeating) {
		this.name = String.format(":%s", name().toLowerCase());
		this.type = type;
		this.repeating = repeating;
	}

	public final String encode() {
		return this.name;
	}

	private static volatile Map<String, IntermediateProperty> MAPPINGS = null;

	private static void initMappings() {
		if (IntermediateProperty.MAPPINGS == null) {
			synchronized (IntermediateProperty.class) {
				if (IntermediateProperty.MAPPINGS == null) {
					Map<String, IntermediateProperty> mappings = new HashMap<String, IntermediateProperty>();
					for (IntermediateProperty a : IntermediateProperty.values()) {
						mappings.put(a.name, a);
					}
					IntermediateProperty.MAPPINGS = Tools.freezeMap(mappings);
				}
			}
		}
	}

	public static IntermediateProperty decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		IntermediateProperty.initMappings();
		IntermediateProperty ret = IntermediateProperty.MAPPINGS.get(name);
		if (ret == null) { throw new IllegalArgumentException(String.format(
			"Failed to decode [%s] into a valid intermediate attribute", name)); }
		return ret;
	}
}