package com.armedia.caliente.store.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfType;

public class CmfTypeAdapter extends XmlAdapter<String, CmfType> {

	@Override
	public CmfType unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v).toUpperCase();
		return CmfType.valueOf(v);
	}

	@Override
	public String marshal(CmfType v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}