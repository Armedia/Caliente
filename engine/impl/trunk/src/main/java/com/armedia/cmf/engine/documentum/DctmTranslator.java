package com.armedia.cmf.engine.documentum;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.cmf.engine.converter.IntermediateAttributes;
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
	private static final Map<StoredObjectType, BidiMap<String, IntermediateAttributes>> MAPPINGS;

	static {
		Map<StoredObjectType, BidiMap<String, IntermediateAttributes>> mappings = new EnumMap<StoredObjectType, BidiMap<String, IntermediateAttributes>>(
			StoredObjectType.class);
		BidiMap<String, IntermediateAttributes> m = null;

		m = new DualHashBidiMap<String, IntermediateAttributes>();
		m.put(DctmAttributes.R_OBJECT_ID, IntermediateAttributes.OBJECT_ID);
		// OBJECT_CLASS (USER)
		// OBJECT_TYPE (DM_USER)
		m.put(DctmAttributes.USER_NAME, IntermediateAttributes.OBJECT_NAME);
		m.put(DctmAttributes.DESCRIPTION, IntermediateAttributes.DESCRIPTION);
		m.put(DctmAttributes.USER_GROUP_NAME, IntermediateAttributes.GROUP);
		m.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttributes.MODIFICATION_DATE);
		m.put(DctmAttributes.DEFAULT_FOLDER, IntermediateAttributes.DEFAULT_FOLDER);
		mappings.put(StoredObjectType.USER, UnmodifiableBidiMap.unmodifiableBidiMap(m));

		m = new DualHashBidiMap<String, IntermediateAttributes>();
		m.put(DctmAttributes.R_OBJECT_ID, IntermediateAttributes.OBJECT_ID);
		// OBJECT_CLASS (GROUP)
		// OBJECT_TYPE (DM_GROUP)
		m.put(DctmAttributes.GROUP_NAME, IntermediateAttributes.OBJECT_NAME);
		m.put(DctmAttributes.DESCRIPTION, IntermediateAttributes.DESCRIPTION);
		m.put(DctmAttributes.OWNER_NAME, IntermediateAttributes.OWNER);
		m.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttributes.MODIFICATION_DATE);
		mappings.put(StoredObjectType.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(m));

		m = new DualHashBidiMap<String, IntermediateAttributes>();
		m.put(DctmAttributes.R_OBJECT_ID, IntermediateAttributes.OBJECT_ID);
		// OBJECT_CLASS (ACL)
		// OBJECT_TYPE (DM_ACL)
		m.put(DctmAttributes.OBJECT_NAME, IntermediateAttributes.OBJECT_NAME);
		m.put(DctmAttributes.DESCRIPTION, IntermediateAttributes.DESCRIPTION);
		m.put(DctmAttributes.OWNER_NAME, IntermediateAttributes.OWNER);
		mappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(m));

		m = new DualHashBidiMap<String, IntermediateAttributes>();
		m.put(DctmAttributes.R_OBJECT_ID, IntermediateAttributes.OBJECT_ID);
		// OBJECT_CLASS (TYPE)
		// OBJECT_TYPE (DM_TYPE)
		m.put(DctmAttributes.NAME, IntermediateAttributes.OBJECT_NAME);
		m.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttributes.MODIFICATION_DATE);
		m.put(DctmAttributes.OWNER, IntermediateAttributes.OWNER);
		mappings.put(StoredObjectType.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(m));

		m = new DualHashBidiMap<String, IntermediateAttributes>();
		m.put(DctmAttributes.R_OBJECT_ID, IntermediateAttributes.OBJECT_ID);
		// OBJECT_CLASS (FORMAT)
		// OBJECT_TYPE (DM_FORMAT)
		m.put(DctmAttributes.NAME, IntermediateAttributes.OBJECT_NAME);
		m.put(DctmAttributes.DESCRIPTION, IntermediateAttributes.DESCRIPTION);
		mappings.put(StoredObjectType.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(m));

		m = new DualHashBidiMap<String, IntermediateAttributes>();
		m.put(DctmAttributes.R_OBJECT_ID, IntermediateAttributes.OBJECT_ID);
		// OBJECT_CLASS (FOLDER)
		// OBJECT_TYPE (DM_FOLDER|DM_CABINET|...)
		m.put(DctmAttributes.OBJECT_NAME, IntermediateAttributes.OBJECT_NAME);
		m.put(DctmAttributes.TITLE, IntermediateAttributes.DESCRIPTION);
		m.put(DctmAttributes.A_CONTENT_TYPE, IntermediateAttributes.CONTENT_TYPE);
		m.put(DctmAttributes.OWNER_NAME, IntermediateAttributes.OWNER);
		m.put(DctmAttributes.OWNER_PERMIT, IntermediateAttributes.OWNER_PERMISSION);
		m.put(DctmAttributes.GROUP_NAME, IntermediateAttributes.GROUP);
		m.put(DctmAttributes.GROUP_PERMIT, IntermediateAttributes.GROUP_PERMISSION);
		m.put(DctmAttributes.R_CREATOR_NAME, IntermediateAttributes.CREATOR);
		m.put(DctmAttributes.R_CREATION_DATE, IntermediateAttributes.CREATE_DATE);
		m.put(DctmAttributes.R_ACCESS_DATE, IntermediateAttributes.ACCESS_DATE);
		m.put(DctmAttributes.R_MODIFIER, IntermediateAttributes.MODIFIER);
		m.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttributes.MODIFICATION_DATE);
		m.put(DctmAttributes.I_FOLDER_ID, IntermediateAttributes.PARENTS);
		m.put(DctmAttributes.R_FOLDER_PATH, IntermediateAttributes.PATHS);
		m.put(DctmAttributes.ACL_DOMAIN, IntermediateAttributes.ACL_REALM);
		m.put(DctmAttributes.ACL_NAME, IntermediateAttributes.ACL_NAME);
		mappings.put(StoredObjectType.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(m));

		m = new DualHashBidiMap<String, IntermediateAttributes>();
		m.put(DctmAttributes.R_OBJECT_ID, IntermediateAttributes.OBJECT_ID);
		// OBJECT_CLASS (FOLDER)
		// OBJECT_TYPE (DM_DOCUMENT|...)
		m.put(DctmAttributes.OBJECT_NAME, IntermediateAttributes.OBJECT_NAME);
		m.put(DctmAttributes.TITLE, IntermediateAttributes.DESCRIPTION);
		m.put(DctmAttributes.A_CONTENT_TYPE, IntermediateAttributes.CONTENT_TYPE);
		m.put(DctmAttributes.R_CONTENT_SIZE, IntermediateAttributes.CONTENT_SIZE);
		m.put(DctmAttributes.OWNER_NAME, IntermediateAttributes.OWNER);
		m.put(DctmAttributes.OWNER_PERMIT, IntermediateAttributes.OWNER_PERMISSION);
		m.put(DctmAttributes.GROUP_NAME, IntermediateAttributes.GROUP);
		m.put(DctmAttributes.GROUP_PERMIT, IntermediateAttributes.GROUP_PERMISSION);
		m.put(DctmAttributes.R_CREATOR_NAME, IntermediateAttributes.CREATOR);
		m.put(DctmAttributes.R_CREATION_DATE, IntermediateAttributes.CREATE_DATE);
		m.put(DctmAttributes.R_ACCESS_DATE, IntermediateAttributes.ACCESS_DATE);
		m.put(DctmAttributes.R_MODIFIER, IntermediateAttributes.MODIFIER);
		m.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttributes.MODIFICATION_DATE);
		m.put(DctmAttributes.I_FOLDER_ID, IntermediateAttributes.PARENTS);
		m.put(DctmAttributes.ACL_DOMAIN, IntermediateAttributes.ACL_REALM);
		m.put(DctmAttributes.ACL_NAME, IntermediateAttributes.ACL_NAME);
		mappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(m));

		m = new DualHashBidiMap<String, IntermediateAttributes>();
		m.put(DctmAttributes.R_OBJECT_ID, IntermediateAttributes.OBJECT_ID);
		// OBJECT_CLASS (CONTENT)
		// OBJECT_TYPE (DMR_CONTENT|...)
		m.put(DctmAttributes.FULL_FORMAT, IntermediateAttributes.CONTENT_TYPE);
		m.put(DctmAttributes.CONTENT_SIZE, IntermediateAttributes.CONTENT_SIZE);
		m.put(DctmAttributes.R_CONTENT_HASH, IntermediateAttributes.CONTENT_HASH);
		m.put(DctmAttributes.PARENT_ID, IntermediateAttributes.PARENTS);
		mappings.put(StoredObjectType.CONTENT, UnmodifiableBidiMap.unmodifiableBidiMap(m));

		MAPPINGS = Tools.freezeMap(mappings);
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

	private BidiMap<String, IntermediateAttributes> getMappings(StoredObjectType type) {
		return DctmTranslator.MAPPINGS.get(type);
	}

	@Override
	public String encodeAttributeName(StoredObjectType type, String attributeName) {
		BidiMap<String, IntermediateAttributes> mappings = getMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS attribute name
			IntermediateAttributes att = mappings.get(attributeName);
			if (att != null) { return att.encode(); }
		}
		return super.encodeAttributeName(type, attributeName);
	}

	@Override
	public String decodeAttributeName(StoredObjectType type, String attributeName) {
		BidiMap<String, IntermediateAttributes> mappings = getMappings(type);
		if (mappings != null) {
			String att = null;
			try {
				// TODO: normalize the intermediate attribute name
				att = mappings.getKey(IntermediateAttributes.decode(attributeName));
			} catch (IllegalArgumentException e) {
				att = null;
			}
			if (att != null) { return att; }
		}
		return super.decodeAttributeName(type, attributeName);
	}

	@Override
	public String encodePropertyName(StoredObjectType type, String attributeName) {
		return super.encodePropertyName(type, attributeName);
	}

	@Override
	public String decodePropertyName(StoredObjectType type, String attributeName) {
		return super.decodePropertyName(type, attributeName);
	}

	@Override
	public StoredObject<IDfValue> decodeObject(StoredObject<IDfValue> rawObject) {
		// TODO: Perhaps perform specific attribute processing here?
		return super.decodeObject(rawObject);
	}

	@Override
	public StoredObject<IDfValue> encodeObject(StoredObject<IDfValue> rawObject) {
		// TODO: Perhaps perform specific attribute processing here?
		return super.encodeObject(rawObject);
	}
}