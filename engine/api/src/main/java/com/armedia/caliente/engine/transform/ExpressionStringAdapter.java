package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

public class ExpressionStringAdapter extends XmlAdapter<String, String> {

	private String trim(String v) {
		return StringUtils.strip(v);
	}

	@Override
	public String unmarshal(String v) throws Exception {
		return trim(v);
	}

	@Override
	public String marshal(String v) throws Exception {
		return trim(v);
	}

}