package com.armedia.caliente.store.xml;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.xml.EnumCodec;

public class CmfObjectArchetypeAdapter extends EnumCodec<CmfObject.Archetype> {
	public CmfObjectArchetypeAdapter() {
		super(CmfObject.Archetype.class);
	}
}