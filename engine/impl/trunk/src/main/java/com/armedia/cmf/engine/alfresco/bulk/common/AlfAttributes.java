package com.armedia.cmf.engine.alfresco.bulk.common;

public enum AlfAttributes {
	//
	//
	;

	public final String name;

	private AlfAttributes() {
		this.name = name().toLowerCase();
	}
}