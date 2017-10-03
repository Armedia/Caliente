package com.armedia.caliente.engine.alfresco.bi;

import java.text.ParseException;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;

public class AlfTranslator extends CmfAttributeTranslator<CmfValue> {

	protected AlfTranslator() {
		super(CmfValue.class, null);
	}

	@Override
	public CmfValue getValue(CmfDataType type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}

	@Override
	public String getDefaultSubtype(CmfType baseType) {
		switch (baseType) {
			case DOCUMENT:
				return "arm:document";
			case FOLDER:
				return "arm:folder";
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