package com.armedia.caliente.store.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfArchetype;

public class CmfTypeAdapter extends XmlAdapter<String, CmfArchetype> {

	@Override
	public CmfArchetype unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v).toUpperCase();
		return CmfArchetype.valueOf(v);
	}

	@Override
	public String marshal(CmfArchetype v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}