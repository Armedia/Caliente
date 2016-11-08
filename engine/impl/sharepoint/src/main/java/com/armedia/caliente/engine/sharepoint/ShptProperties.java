package com.armedia.caliente.engine.sharepoint;

public enum ShptProperties {

	//
	//
	;

	public final String name;

	private ShptProperties() {
		this.name = name().toLowerCase();
	}
}