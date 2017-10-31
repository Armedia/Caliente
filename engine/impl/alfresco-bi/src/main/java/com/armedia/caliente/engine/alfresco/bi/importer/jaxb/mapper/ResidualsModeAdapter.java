package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

public class ResidualsModeAdapter extends XmlAdapter<String, ResidualsMode> {

	@Override
	public ResidualsMode unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v);
		v = StringUtils.upperCase(v);
		return ResidualsMode.valueOf(v);
	}

	@Override
	public String marshal(ResidualsMode v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}