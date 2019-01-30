package com.armedia.caliente.store.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfValue;

public class CmfValueTypeAdapter extends XmlAdapter<String, CmfValue.Type> {

	@Override
	public CmfValue.Type unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v).toUpperCase();
		return CmfValue.Type.valueOf(v);
	}

	@Override
	public String marshal(CmfValue.Type v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}