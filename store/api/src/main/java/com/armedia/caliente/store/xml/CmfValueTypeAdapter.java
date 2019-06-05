package com.armedia.caliente.store.xml;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.xml.AbstractEnumAdapter;

public class CmfValueTypeAdapter extends AbstractEnumAdapter<CmfValue.Type> {
	public CmfValueTypeAdapter() {
		super(CmfValue.Type.class);
	}
}