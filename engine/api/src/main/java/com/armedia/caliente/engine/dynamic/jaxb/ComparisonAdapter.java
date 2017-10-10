package com.armedia.caliente.engine.dynamic.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ComparisonAdapter extends XmlAdapter<String, Comparison> {

	@Override
	public Comparison unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		return Comparison.get(v.trim());
	}

	@Override
	public String marshal(Comparison v) throws Exception {
		if (v == null) { return null; }
		return v.name().toLowerCase();
	}

}