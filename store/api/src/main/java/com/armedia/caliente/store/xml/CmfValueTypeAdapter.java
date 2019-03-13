package com.armedia.caliente.store.xml;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.XmlEnumAdapter;

public class CmfValueTypeAdapter extends XmlEnumAdapter<CmfValue.Type> {
	public CmfValueTypeAdapter() {
		super(CmfValue.Type.class);
	}
}