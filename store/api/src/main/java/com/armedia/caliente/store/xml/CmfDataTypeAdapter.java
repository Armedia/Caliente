package com.armedia.caliente.store.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfDataType;

public class CmfDataTypeAdapter extends XmlAdapter<String, CmfDataType> {

	@Override
	public CmfDataType unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v).toUpperCase();
		return CmfDataType.valueOf(v);
	}

	@Override
	public String marshal(CmfDataType v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}