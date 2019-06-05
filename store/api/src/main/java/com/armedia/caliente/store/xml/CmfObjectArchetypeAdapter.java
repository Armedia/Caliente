package com.armedia.caliente.store.xml;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.xml.AbstractEnumAdapter;

public class CmfObjectArchetypeAdapter extends AbstractEnumAdapter<CmfObject.Archetype> {
	public CmfObjectArchetypeAdapter() {
		super(CmfObject.Archetype.class);
	}
}