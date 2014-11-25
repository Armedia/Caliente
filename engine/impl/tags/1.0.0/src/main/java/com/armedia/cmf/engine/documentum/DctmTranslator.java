package com.armedia.cmf.engine.documentum;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

public final class DctmTranslator extends ObjectStorageTranslator<IDfPersistentObject, IDfValue> {

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
	protected Class<IDfPersistentObject> doDecodeObjectType(StoredObjectType type)
		throws UnsupportedObjectTypeException {
		return null;
	}

	@Override
	protected String doGetObjectId(IDfPersistentObject object) throws DfException {
		return object.getObjectId().getId();
	}

}