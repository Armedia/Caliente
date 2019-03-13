package com.armedia.caliente.store.xml;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.XmlEnumAdapter;

public class CmfObjectArchetypeAdapter extends XmlEnumAdapter<CmfObject.Archetype> {
	public CmfObjectArchetypeAdapter() {
		super(CmfObject.Archetype.class);
	}
}