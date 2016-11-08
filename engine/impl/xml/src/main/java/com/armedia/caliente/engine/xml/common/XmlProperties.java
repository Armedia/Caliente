package com.armedia.caliente.engine.xml.common;

public enum XmlProperties {
	//
	TARGET_PATHS,
	TARGET_PARENTS,
	CONTENTS,
	CURRENT_VERSION,
	//
	;

	public final String name;

	private XmlProperties() {
		this.name = name().toLowerCase();
	}
}