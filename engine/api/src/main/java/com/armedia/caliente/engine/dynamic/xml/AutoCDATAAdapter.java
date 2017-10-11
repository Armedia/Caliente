package com.armedia.caliente.engine.dynamic.xml;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class AutoCDATAAdapter extends XmlAdapter<String, String> {

	private static final Pattern CDATA_CHECKER = Pattern.compile("[&<>]");

	@Override
	public String marshal(String value) throws Exception {
		if (AutoCDATAAdapter.CDATA_CHECKER.matcher(value).find()) {
			value = String.format("<![CDATA[%s]]>", value);
		}
		return value;
	}

	@Override
	public String unmarshal(String value) throws Exception {
		return value;
	}

}