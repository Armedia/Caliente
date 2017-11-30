package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.PrincipalType;

public class PrincipalTypeAdapter extends XmlAdapter<String, PrincipalType> {

	@Override
	public PrincipalType unmarshal(String v) throws Exception {
		if (v == null) { return null; }
		v = StringUtils.strip(v);
		v = StringUtils.upperCase(v);
		return PrincipalType.valueOf(v);
	}

	@Override
	public String marshal(PrincipalType v) throws Exception {
		if (v == null) { return null; }
		return v.name();
	}

}