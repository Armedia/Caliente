package com.armedia.caliente.store.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfObject;

public class CmfObjectArchetypeAdapter extends XmlAdapter<String, CmfObject.Archetype> {

	@Override
	public CmfObject.Archetype unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v).toUpperCase();
		return CmfObject.Archetype.valueOf(v);
	}

	@Override
	public String marshal(CmfObject.Archetype v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}