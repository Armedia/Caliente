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
public enum IntermediateProperty implements Supplier<String>, CmfEncodeableName {
	// CMIS-inspired properties
	PATH(PropertyIds.PATH, CmfValueType.STRING),
	PARENT_ID(PropertyIds.PARENT_ID, CmfValueType.ID),
	CONTENT_STREAM_ID(PropertyIds.CONTENT_STREAM_ID, CmfValueType.STRING),

	// Non-CMIS properties
	PARENT_TREE_IDS(CmfValueType.STRING),
	LATEST_PARENT_TREE_IDS(CmfValueType.STRING),
	ACL_ID(CmfValueType.ID),
	ACL_INHERITANCE(CmfValueType.BOOLEAN),
	ACL_OWNER(CmfValueType.STRING),
	ACL_OBJECT_ID(CmfValueType.STRING),
	ACL_ACCESSOR_NAME(CmfValueType.STRING),
	ACL_ACCESSOR_TYPE(CmfValueType.STRING),
	ACL_ACCESSOR_ACTIONS(CmfValueType.STRING),
	VERSION_TREE_ROOT(CmfValueType.BOOLEAN),
	VERSION_COUNT(CmfValueType.INTEGER),
	VERSION_HEAD_INDEX(CmfValueType.INTEGER),
	VERSION_INDEX(CmfValueType.INTEGER),
	VERSION_PATCHES(CmfValueType.STRING),
	IS_NEWEST_VERSION(CmfValueType.BOOLEAN),
	IS_UNFILED(CmfValueType.BOOLEAN),
	PATCH_ANTECEDENT(CmfValueType.STRING),
	USERS_WITH_DEFAULT_FOLDER(CmfValueType.STRING),
	USERS_DEFAULT_FOLDER_PATHS(CmfValueType.STRING),
	USERS_WITH_DEFAULT_ACL(CmfValueType.STRING),
	USERS_WITH_DEFAULT_GROUP(CmfValueType.STRING),
	GROUPS_WITH_DEFAULT_FOLDER(CmfValueType.STRING),
	ORIG_ATTR_NAME(CmfValueType.STRING),
	MAPPED_ATTR_NAME(CmfValueType.STRING),
	VDOC_HISTORY(CmfValueType.BOOLEAN),
	VDOC_MEMBER(CmfValueType.STRING),
	DEFAULT_ACL(CmfValueType.STRING),
	DEFAULT_ASPECTS(CmfValueType.STRING),
	DEFAULT_STORAGE(CmfValueType.STRING),
	IS_REFERENCE(CmfValueType.BOOLEAN),
	REF_TARGET(CmfValueType.STRING),
	REF_VERSION(CmfValueType.STRING),
	//
	;

	private final String name;
	public final CmfValueType type;
	public final boolean repeating;

	private IntermediateProperty(CmfValueType type) {
		this(null, type, false);
	}

	private IntermediateProperty(String propertyId, CmfValueType type) {
		this(propertyId, type, false);
	}

	private IntermediateProperty(CmfValueType type, boolean repeating) {
		this(null, type, repeating);
	}

	private IntermediateProperty(String propertyId, CmfValueType type, boolean repeating) {
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
		if (ret == null) {
			throw new IllegalArgumentException(
				String.format("Failed to decode [%s] into a valid intermediate property", name));
		}
		return ret;
	}
}