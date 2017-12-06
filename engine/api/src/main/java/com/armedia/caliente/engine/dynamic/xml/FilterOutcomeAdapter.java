package com.armedia.caliente.engine.dynamic.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

public class FilterOutcomeAdapter extends XmlAdapter<String, FilterOutcome> {

	@Override
	public FilterOutcome unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v);
		v = StringUtils.upperCase(v);
		return FilterOutcome.valueOf(v);
	}

	@Override
	public String marshal(FilterOutcome v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}