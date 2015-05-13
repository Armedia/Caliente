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
public enum IntermediateProperty implements Mappable {
	// CMIS-inspired properties
	PATH(PropertyIds.PATH, StoredDataType.STRING),
	PARENT_ID(PropertyIds.PARENT_ID, StoredDataType.STRING),
	CONTENT_STREAM_ID(PropertyIds.CONTENT_STREAM_ID, StoredDataType.STRING),
	IS_LATEST_VERSION(PropertyIds.IS_LATEST_VERSION, StoredDataType.BOOLEAN),

	// Non-CMIS properties
	DEFAULT_PATH(StoredDataType.STRING),
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

	private static volatile Map<String, IntermediateProperty> MAPPINGS = null;

	private static void initMappings() {
		if (IntermediateProperty.MAPPINGS == null) {
			synchronized (IntermediateProperty.class) {
				if (IntermediateProperty.MAPPINGS == null) {
					IntermediateProperty.MAPPINGS = Tools.freezeMap(MappingManager.createMappings(
						IntermediateProperty.class, IntermediateProperty.values()));
				}
			}
		}
	}

	public static IntermediateProperty decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		IntermediateProperty.initMappings();
		IntermediateProperty ret = IntermediateProperty.MAPPINGS.get(name);
		if (ret == null) { throw new IllegalArgumentException(String.format(
			"Failed to decode [%s] into a valid intermediate property", name)); }
		return ret;
	}
}