package com.armedia.cmf.engine.alfresco.bulk.common;

import java.text.ParseException;

import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueCodec;

public class AlfTranslator extends CmfAttributeTranslator<CmfValue> {

	@Override
	public CmfValue getValue(CmfDataType type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}

	@Override
	public String getDefaultSubtype(CmfType baseType) {
		switch (baseType) {
			case DOCUMENT:
				return "jsap:document";
			case FOLDER:
				return "jsap:folder";
			default:
				break;
		}
		return baseType.name();
	}

	@Override
	public CmfValueCodec<CmfValue> getCodec(CmfDataType type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}
}