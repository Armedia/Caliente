package com.armedia.caliente.engine.xml.common;

public enum XmlAttributes {
	//
	//
	;

	public final String name;

	private XmlAttributes() {
		this.name = name().toLowerCase();
	}
}