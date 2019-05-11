package com.armedia.caliente.store.xml;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.xml.EnumCodec;

public class CmfValueTypeAdapter extends EnumCodec<CmfValue.Type> {
	public CmfValueTypeAdapter() {
		super(CmfValue.Type.class);
	}
}