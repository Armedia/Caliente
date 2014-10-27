package com.delta.cmsmf.cms;

import com.armedia.cmf.storage.StoredObjectType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.IDfValue;

public class DctmUtils {

	public static final IDfValue[] NO_VALUES = new IDfValue[0];

	public static StoredObjectType decodeObjectType(IDfPersistentObject targetObject)
		throws UnsupportedObjectTypeException {
		return null;
	}

	public static StoredObjectType decodeObjectType(Class<?> dfClass) {
		return null;
	}
}