package com.delta.cmsmf.cms;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueCodec;
import com.documentum.fc.common.IDfValue;

public final class DctmTranslator extends ObjectStorageTranslator<IDfValue> {

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
		switch (type) {
			case ACL:
				return DctmObjectType.ACL;
			case USER:
				return DctmObjectType.USER;
			case GROUP:
				return DctmObjectType.GROUP;
			case TYPE:
				return DctmObjectType.TYPE;
			case FOLDER:
				return DctmObjectType.FOLDER;
			case FORMAT:
				return DctmObjectType.FORMAT;
			case CONTENT_STREAM:
				return DctmObjectType.CONTENT;
			case DOCUMENT:
				return DctmObjectType.DOCUMENT;
			default:
				return null;
		}
	}

	@Override
	public StoredValueCodec<IDfValue> getCodec(StoredDataType type) {
		return DctmTranslator.translateType(type);
	}

	public static final ObjectStorageTranslator<IDfValue> INSTANCE = new DctmTranslator();

}