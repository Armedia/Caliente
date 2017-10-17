package com.armedia.caliente.engine.dynamic.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

public class ComparisonAdapter extends XmlAdapter<String, Comparison> {

	@Override
	public Comparison unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v);
		return Comparison.get(v);
	}

	@Override
	public String marshal(Comparison v) throws Exception {
		if (v == null) { return null; }
		return v.name().toLowerCase();
	}

}