package com.armedia.caliente.store;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.armedia.caliente.store.CmfType;

public class CmfTypeAdapter extends XmlAdapter<String, CmfType> {

	@Override
	public CmfType unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = v.trim().toUpperCase();
		return CmfType.decodeString(v.toUpperCase());
	}

	@Override
	public String marshal(CmfType v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}