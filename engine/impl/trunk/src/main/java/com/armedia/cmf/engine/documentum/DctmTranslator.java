package com.armedia.cmf.engine.documentum;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

public final class DctmTranslator extends ObjectStorageTranslator<IDfPersistentObject, IDfValue> {
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
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (USER)
		// OBJECT_TYPE (DM_USER)
		am.put(DctmAttributes.USER_NAME, IntermediateAttribute.OBJECT_NAME);
		am.put(DctmAttributes.DESCRIPTION, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.USER_GROUP_NAME, IntermediateAttribute.GROUP);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.MODIFICATION_DATE);
		am.put(DctmAttributes.DEFAULT_FOLDER, IntermediateAttribute.DEFAULT_FOLDER);
		attributeMappings.put(StoredObjectType.USER, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.USER, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (GROUP)
		// OBJECT_TYPE (DM_GROUP)
		am.put(DctmAttributes.GROUP_NAME, IntermediateAttribute.OBJECT_NAME);
		am.put(DctmAttributes.DESCRIPTION, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.OWNER_NAME, IntermediateAttribute.OWNER);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.MODIFICATION_DATE);
		attributeMappings.put(StoredObjectType.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (ACL)
		// OBJECT_TYPE (DM_ACL)
		am.put(DctmAttributes.OBJECT_NAME, IntermediateAttribute.OBJECT_NAME);
		am.put(DctmAttributes.DESCRIPTION, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.OWNER_NAME, IntermediateAttribute.OWNER);
		attributeMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (TYPE)
		// OBJECT_TYPE (DM_TYPE)
		am.put(DctmAttributes.NAME, IntermediateAttribute.OBJECT_NAME);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.MODIFICATION_DATE);
		am.put(DctmAttributes.OWNER, IntermediateAttribute.OWNER);
		attributeMappings.put(StoredObjectType.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (FORMAT)
		// OBJECT_TYPE (DM_FORMAT)
		am.put(DctmAttributes.NAME, IntermediateAttribute.OBJECT_NAME);
		am.put(DctmAttributes.DESCRIPTION, IntermediateAttribute.DESCRIPTION);
		attributeMappings.put(StoredObjectType.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (FOLDER)
		// OBJECT_TYPE (DM_FOLDER|DM_CABINET|...)
		am.put(DctmAttributes.OBJECT_NAME, IntermediateAttribute.OBJECT_NAME);
		am.put(DctmAttributes.TITLE, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.A_CONTENT_TYPE, IntermediateAttribute.CONTENT_TYPE);
		am.put(DctmAttributes.OWNER_NAME, IntermediateAttribute.OWNER);
		am.put(DctmAttributes.OWNER_PERMIT, IntermediateAttribute.OWNER_PERMISSION);
		am.put(DctmAttributes.GROUP_NAME, IntermediateAttribute.GROUP);
		am.put(DctmAttributes.GROUP_PERMIT, IntermediateAttribute.GROUP_PERMISSION);
		am.put(DctmAttributes.R_CREATOR_NAME, IntermediateAttribute.CREATOR);
		am.put(DctmAttributes.R_CREATION_DATE, IntermediateAttribute.CREATE_DATE);
		am.put(DctmAttributes.R_ACCESS_DATE, IntermediateAttribute.ACCESS_DATE);
		am.put(DctmAttributes.R_MODIFIER, IntermediateAttribute.MODIFIER);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.MODIFICATION_DATE);
		am.put(DctmAttributes.I_FOLDER_ID, IntermediateAttribute.PARENTS);
		am.put(DctmAttributes.R_FOLDER_PATH, IntermediateAttribute.PATHS);
		am.put(DctmAttributes.ACL_DOMAIN, IntermediateAttribute.ACL_REALM);
		am.put(DctmAttributes.ACL_NAME, IntermediateAttribute.ACL_NAME);
		attributeMappings.put(StoredObjectType.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (FOLDER)
		// OBJECT_TYPE (DM_DOCUMENT|...)
		am.put(DctmAttributes.OBJECT_NAME, IntermediateAttribute.OBJECT_NAME);
		am.put(DctmAttributes.TITLE, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.A_CONTENT_TYPE, IntermediateAttribute.CONTENT_TYPE);
		am.put(DctmAttributes.R_CONTENT_SIZE, IntermediateAttribute.CONTENT_SIZE);
		am.put(DctmAttributes.OWNER_NAME, IntermediateAttribute.OWNER);
		am.put(DctmAttributes.OWNER_PERMIT, IntermediateAttribute.OWNER_PERMISSION);
		am.put(DctmAttributes.GROUP_NAME, IntermediateAttribute.GROUP);
		am.put(DctmAttributes.GROUP_PERMIT, IntermediateAttribute.GROUP_PERMISSION);
		am.put(DctmAttributes.R_CREATOR_NAME, IntermediateAttribute.CREATOR);
		am.put(DctmAttributes.R_CREATION_DATE, IntermediateAttribute.CREATE_DATE);
		am.put(DctmAttributes.R_ACCESS_DATE, IntermediateAttribute.ACCESS_DATE);
		am.put(DctmAttributes.R_MODIFIER, IntermediateAttribute.MODIFIER);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.MODIFICATION_DATE);
		am.put(DctmAttributes.I_FOLDER_ID, IntermediateAttribute.PARENTS);
		am.put(DctmAttributes.ACL_DOMAIN, IntermediateAttribute.ACL_REALM);
		am.put(DctmAttributes.ACL_NAME, IntermediateAttribute.ACL_NAME);
		attributeMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (CONTENT)
		// OBJECT_TYPE (DMR_CONTENT|...)
		am.put(DctmAttributes.FULL_FORMAT, IntermediateAttribute.CONTENT_TYPE);
		am.put(DctmAttributes.CONTENT_SIZE, IntermediateAttribute.CONTENT_SIZE);
		am.put(DctmAttributes.R_CONTENT_HASH, IntermediateAttribute.CONTENT_HASH);
		am.put(DctmAttributes.PARENT_ID, IntermediateAttribute.PARENTS);
		attributeMappings.put(StoredObjectType.CONTENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.CONTENT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
		PROPERTY_MAPPINGS = Tools.freezeMap(propertyMappings);
	}

	private DctmTranslator() {
		// Nobody can instantiate
	}

	public static DctmDataType translateType(StoredDataType type) {
		switch (type) {
			case BOOLEAN:
				return DctmDataType.DF_BOOLEAN;
			case INTEGER:
				return DctmDataType.DF_INTEGER;
			case STRING:
				return DctmDataType.DF_STRING;
			case DOUBLE:
				return DctmDataType.DF_DOUBLE;
			case ID:
				return DctmDataType.DF_ID;
			case TIME:
				return DctmDataType.DF_TIME;
			default:
				return DctmDataType.DF_UNDEFINED;
		}
	}

	public static DctmObjectType translateType(StoredObjectType type) {
		for (DctmObjectType t : DctmObjectType.values()) {
			if (t.getStoredObjectType() == type) { return t; }
		}
		return null;
	}

	@Override
	public StoredValueCodec<IDfValue> getCodec(StoredDataType type) {
		return DctmTranslator.translateType(type);
	}

	public static final ObjectStorageTranslator<IDfPersistentObject, IDfValue> INSTANCE = new DctmTranslator();

	@Override
	protected StoredObjectType doDecodeObjectType(IDfPersistentObject object) throws UnsupportedObjectTypeException {
		return null;
	}

	@Override
	protected Class<? extends IDfPersistentObject> doDecodeObjectType(StoredObjectType type)
		throws UnsupportedObjectTypeException {
		return null;
	}

	@Override
	protected String doGetObjectId(IDfPersistentObject object) throws DfException {
		return object.getObjectId().getId();
	}

	private BidiMap<String, IntermediateAttribute> getAttributeMappings(StoredObjectType type) {
		return DctmTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	private BidiMap<String, IntermediateProperty> getPropertyMappings(StoredObjectType type) {
		return DctmTranslator.PROPERTY_MAPPINGS.get(type);
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
	public StoredObject<IDfValue> decodeObject(StoredObject<IDfValue> rawObject) {
		// TODO: Perhaps perform specific attribute and property processing here?
		return super.decodeObject(rawObject);
	}

	@Override
	public StoredObject<IDfValue> encodeObject(StoredObject<IDfValue> rawObject) {
		// TODO: Perhaps perform specific attribute and property processing here?
		return super.encodeObject(rawObject);
	}
}