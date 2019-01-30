package com.armedia.caliente.store.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfValueType;

public class CmfValueTypeAdapter extends XmlAdapter<String, CmfValueType> {

	@Override
	public CmfValueType unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v).toUpperCase();
		return CmfValueType.valueOf(v);
	}

	@Override
	public String marshal(CmfValueType v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}