package com.armedia.caliente.engine.alfresco.bi;

import java.text.ParseException;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;

public class AlfTranslator extends CmfAttributeTranslator<CmfValue> {

	public AlfTranslator() {
		super(CmfValue.class, null);
	}

	@Override
	public CmfValue getValue(CmfDataType type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}

	@Override
	public CmfValueCodec<CmfValue> getCodec(CmfDataType type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}
}