package com.armedia.cmf.engine.xml.common;

public enum XmlAttributes {
	//
	//
	;

	public final String name;

	private XmlAttributes() {
		this.name = name().toLowerCase();
	}
}