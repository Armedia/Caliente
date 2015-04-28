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
public enum IntermediateProperty {
	// CMIS-inspired properties
	TARGET_PATHS(PropertyIds.PATH, StoredDataType.STRING),
	TARGET_PARENTS(PropertyIds.PARENT_ID, StoredDataType.STRING),
	CONTENTS(PropertyIds.CONTENT_STREAM_ID, StoredDataType.STRING),

	// Non-CMIS properties
	USERS_WITH_DEFAULT_GROUP(StoredDataType.ID),
	VERSION_PATCHES(StoredDataType.STRING),
	PATCH_ANTECEDENT(StoredDataType.STRING),
	USERS_WITH_DEFAULT_FOLDER(StoredDataType.STRING),
	USERS_DEFAULT_FOLDER_PATHS(StoredDataType.STRING),
	//
	;

	private final String name;
	public final StoredDataType type;
	public final boolean repeating;

	private IntermediateProperty(StoredDataType type) {
		this(null, type, false);
	}

	private IntermediateProperty(String propertyId, StoredDataType type) {
		this(propertyId, type, false);
	}

	private IntermediateProperty(StoredDataType type, boolean repeating) {
		this(null, type, repeating);
	}

	private IntermediateProperty(String propertyId, StoredDataType type, boolean repeating) {
		this.name = (propertyId != null ? propertyId : String.format(":%s", name().toLowerCase()));
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
						IntermediateProperty b = mappings.put(a.name, a);
						if (b != null) { throw new IllegalStateException(String.format(
							"Both intermediate properties %s and %s resolve the same mapping name (%s|%s)", a, b,
							a.name, b.name)); }
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