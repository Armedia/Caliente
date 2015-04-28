/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public final class ShptTranslator extends ObjectStorageTranslator<StoredValue> {

	private static final Map<StoredObjectType, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;
	private static final Map<StoredObjectType, BidiMap<String, IntermediateProperty>> PROPERTY_MAPPINGS;

	static {
		Map<StoredObjectType, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<StoredObjectType, BidiMap<String, IntermediateAttribute>>(
			StoredObjectType.class);
		Map<StoredObjectType, BidiMap<String, IntermediateProperty>> propertyMappings = new EnumMap<StoredObjectType, BidiMap<String, IntermediateProperty>>(
			StoredObjectType.class);

		BidiMap<String, IntermediateAttribute> am = null;
		BidiMap<String, IntermediateProperty> pm = null;

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (USER)
		// OBJECT_TYPE_ID (DM_USER)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.LOGIN_NAME.name, IntermediateAttribute.LOGIN_NAME);
		am.put(ShptAttributes.LOGIN_DOMAIN.name, IntermediateAttribute.LOGIN_REALM);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		attributeMappings.put(StoredObjectType.USER, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.USER, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (GROUP)
		// OBJECT_TYPE_ID (DM_GROUP)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.GROUP_OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		attributeMappings.put(StoredObjectType.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (ACL)
		// OBJECT_TYPE_ID (DM_ACL)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		attributeMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (TYPE)
		// OBJECT_TYPE_ID (DM_TYPE)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		attributeMappings.put(StoredObjectType.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (FORMAT)
		// OBJECT_TYPE_ID (DM_FORMAT)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		attributeMappings.put(StoredObjectType.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (FOLDER)
		// OBJECT_TYPE_ID (DM_FOLDER|DM_CABINET|...)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.TITLE.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.CONTENT_TYPE.name, IntermediateAttribute.CONTENT_STREAM_MIME_TYPE);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.GROUP.name, IntermediateAttribute.GROUP);
		am.put(ShptAttributes.CREATOR.name, IntermediateAttribute.CREATED_BY);
		am.put(ShptAttributes.CREATE_DATE.name, IntermediateAttribute.CREATION_DATE);
		am.put(ShptAttributes.ACCESS_DATE.name, IntermediateAttribute.LAST_ACCESS_DATE);
		am.put(ShptAttributes.MODIFIER.name, IntermediateAttribute.LAST_MODIFIED_BY);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(ShptAttributes.PARENTS.name, IntermediateAttribute.PARENT_ID);
		am.put(ShptAttributes.PATHS.name, IntermediateAttribute.PATH);
		attributeMappings.put(StoredObjectType.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		pm.put(ShptProperties.TARGET_PATHS.name, IntermediateProperty.TARGET_PATHS);
		pm.put(ShptProperties.TARGET_PARENTS.name, IntermediateProperty.TARGET_PARENTS);
		pm.put(ShptProperties.USERS_WITH_DEFAULT_FOLDER.name, IntermediateProperty.USERS_WITH_DEFAULT_FOLDER);
		pm.put(ShptProperties.USERS_DEFAULT_FOLDER_PATHS.name, IntermediateProperty.USERS_DEFAULT_FOLDER_PATHS);
		propertyMappings.put(StoredObjectType.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (FOLDER)
		// OBJECT_TYPE_ID (DM_DOCUMENT|...)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.TITLE.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.CONTENT_TYPE.name, IntermediateAttribute.CONTENT_STREAM_MIME_TYPE);
		am.put(ShptAttributes.CONTENT_SIZE.name, IntermediateAttribute.CONTENT_STREAM_LENGTH);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.CREATOR.name, IntermediateAttribute.CREATED_BY);
		am.put(ShptAttributes.CREATE_DATE.name, IntermediateAttribute.CREATION_DATE);
		am.put(ShptAttributes.MODIFIER.name, IntermediateAttribute.LAST_MODIFIED_BY);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(ShptAttributes.PARENTS.name, IntermediateAttribute.PARENT_ID);
		am.put(ShptAttributes.VERSION.name, IntermediateAttribute.VERSION_LABEL);
		am.put(ShptAttributes.VERSION_TREE.name, IntermediateAttribute.VERSION_SERIES_ID);
		am.put(ShptAttributes.VERSION_PRIOR.name, IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		pm.put(ShptProperties.TARGET_PATHS.name, IntermediateProperty.TARGET_PATHS);
		pm.put(ShptProperties.TARGET_PARENTS.name, IntermediateProperty.TARGET_PARENTS);
		pm.put(ShptProperties.CONTENTS.name, IntermediateProperty.CONTENTS);
		pm.put(ShptProperties.VERSION_PATCHES.name, IntermediateProperty.VERSION_PATCHES);
		pm.put(ShptProperties.PATCH_ANTECEDENT.name, IntermediateProperty.PATCH_ANTECEDENT);
		propertyMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (CONTENT)
		// OBJECT_TYPE_ID (DMR_CONTENT|...)
		am.put(ShptAttributes.CONTENT_TYPE.name, IntermediateAttribute.CONTENT_STREAM_MIME_TYPE);
		am.put(ShptAttributes.CONTENT_SIZE.name, IntermediateAttribute.CONTENT_STREAM_LENGTH);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		attributeMappings.put(StoredObjectType.CONTENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		pm.put(ShptProperties.TARGET_PATHS.name, IntermediateProperty.TARGET_PATHS);
		pm.put(ShptProperties.TARGET_PARENTS.name, IntermediateProperty.TARGET_PARENTS);
		pm.put(ShptProperties.CONTENTS.name, IntermediateProperty.CONTENTS);
		pm.put(ShptProperties.VERSION_PATCHES.name, IntermediateProperty.VERSION_PATCHES);
		pm.put(ShptProperties.PATCH_ANTECEDENT.name, IntermediateProperty.PATCH_ANTECEDENT);
		propertyMappings.put(StoredObjectType.CONTENT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
		PROPERTY_MAPPINGS = Tools.freezeMap(propertyMappings);
	}

	public static ShptTranslator INSTANCE = new ShptTranslator();

	private ShptTranslator() {
		// Avoid instantiation
	}

	@Override
	public StoredValueCodec<StoredValue> getCodec(StoredDataType type) {
		return ObjectStorageTranslator.getStoredValueCodec(type);
	}

	private BidiMap<String, IntermediateAttribute> getAttributeMappings(StoredObjectType type) {
		return ShptTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	private BidiMap<String, IntermediateProperty> getPropertyMappings(StoredObjectType type) {
		return ShptTranslator.PROPERTY_MAPPINGS.get(type);
	}

	@Override
	public String encodeAttributeName(StoredObjectType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS attribute name
			IntermediateAttribute att = mappings.get(attributeName);
			if (att != null) { return att.encode(); }
		}
		return super.encodeAttributeName(type, attributeName);
	}

	@Override
	public String decodeAttributeName(StoredObjectType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
		if (mappings != null) {
			String att = null;
			try {
				// TODO: normalize the intermediate attribute name
				att = mappings.getKey(IntermediateAttribute.decode(attributeName));
			} catch (IllegalArgumentException e) {
				att = null;
			}
			if (att != null) { return att; }
		}
		return super.decodeAttributeName(type, attributeName);
	}

	@Override
	public String encodePropertyName(StoredObjectType type, String propertyName) {
		BidiMap<String, IntermediateProperty> mappings = getPropertyMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS property name
			IntermediateProperty prop = mappings.get(propertyName);
			if (prop != null) { return prop.encode(); }
		}
		return super.encodePropertyName(type, propertyName);
	}

	@Override
	public String decodePropertyName(StoredObjectType type, String propertyName) {
		BidiMap<String, IntermediateProperty> mappings = getPropertyMappings(type);
		if (mappings != null) {
			String prop = null;
			try {
				// TODO: normalize the intermediate property name
				prop = mappings.getKey(IntermediateProperty.decode(propertyName));
			} catch (IllegalArgumentException e) {
				prop = null;
			}
			if (prop != null) { return prop; }
		}
		return super.decodePropertyName(type, propertyName);
	}

	@Override
	public StoredValue getValue(StoredDataType type, Object value) throws ParseException {
		try {
			return new StoredValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException("Exception raised while creating a new value", e);
		}
	}
}