package com.armedia.cmf.engine.sharepoint;

public enum ShptProperties {

	//
	//
	;

	public final String name;

	private ShptProperties() {
		this.name = name().toLowerCase();
	}
}