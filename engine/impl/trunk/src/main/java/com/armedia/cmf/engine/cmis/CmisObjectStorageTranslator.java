package com.armedia.cmf.engine.cmis;

import java.text.ParseException;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueCodec;

public class CmisObjectStorageTranslator extends ObjectStorageTranslator<StoredValue> {

	@Override
	protected String doGetObjectId(Object object) throws Exception {
		return null;
	}

	@Override
	public StoredValueCodec<StoredValue> getCodec(StoredDataType type) {
		return null;
	}

	@Override
	public StoredValue getValue(StoredDataType type, Object value) throws ParseException {
		return null;
	}
}