/**
 *
 */

package com.armedia.cmf.engine.converter;

import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.cmf.engine.converter.MappingManager.Mappable;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfEncodeableName;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public enum IntermediateProperty implements Mappable, CmfEncodeableName {
	// CMIS-inspired properties
	PATH(PropertyIds.PATH, CmfDataType.STRING),
	PARENT_ID(PropertyIds.PARENT_ID, CmfDataType.ID),
	CONTENT_STREAM_ID(PropertyIds.CONTENT_STREAM_ID, CmfDataType.STRING),

	// Non-CMIS properties
	PARENT_TREE_IDS(CmfDataType.STRING),
	ACL_ID(CmfDataType.ID),
	ACL_INHERITANCE(CmfDataType.BOOLEAN),
	ACL_OWNER(CmfDataType.STRING),
	ACL_OBJECT_ID(CmfDataType.STRING),
	ACL_ACCESSOR_NAME(CmfDataType.STRING),
	ACL_ACCESSOR_TYPE(CmfDataType.STRING),
	ACL_ACCESSOR_ACTIONS(CmfDataType.STRING),
	USERS_WITH_DEFAULT_GROUP(CmfDataType.ID),
	VERSION_TREE_ROOT(CmfDataType.BOOLEAN),
	VERSION_COUNT(CmfDataType.INTEGER),
	VERSION_INDEX(CmfDataType.INTEGER),
	VERSION_PATCHES(CmfDataType.STRING),
	IS_NEWEST_VERSION(CmfDataType.BOOLEAN),
	PATCH_ANTECEDENT(CmfDataType.STRING),
	USERS_WITH_DEFAULT_FOLDER(CmfDataType.STRING),
	USERS_DEFAULT_FOLDER_PATHS(CmfDataType.STRING),
	ORIG_ATTR_NAME(CmfDataType.STRING),
	MAPPED_ATTR_NAME(CmfDataType.STRING),
	VDOC_MEMBER(CmfDataType.STRING),

	// I swore never to do this again, but no choice here...
	// A kludge for JSAP...
	JSAP_PARENT_TREE_IDS(CmfDataType.STRING),

	//
	;

	private final String name;
	public final CmfDataType type;
	public final boolean repeating;

	private IntermediateProperty(CmfDataType type) {
		this(null, type, false);
	}

	private IntermediateProperty(String propertyId, CmfDataType type) {
		this(propertyId, type, false);
	}

	private IntermediateProperty(CmfDataType type, boolean repeating) {
		this(null, type, repeating);
	}

	private IntermediateProperty(String propertyId, CmfDataType type, boolean repeating) {
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

	private static volatile Map<String, IntermediateProperty> MAPPINGS = null;

	private static void initMappings() {
		if (IntermediateProperty.MAPPINGS == null) {
			synchronized (IntermediateProperty.class) {
				if (IntermediateProperty.MAPPINGS == null) {
					IntermediateProperty.MAPPINGS = Tools.freezeMap(
						MappingManager.createMappings(IntermediateProperty.class, IntermediateProperty.values()));
				}
			}
		}
	}

	public static IntermediateProperty decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		IntermediateProperty.initMappings();
		IntermediateProperty ret = IntermediateProperty.MAPPINGS.get(name);
		if (ret == null) { throw new IllegalArgumentException(
			String.format("Failed to decode [%s] into a valid intermediate property", name)); }
		return ret;
	}
}