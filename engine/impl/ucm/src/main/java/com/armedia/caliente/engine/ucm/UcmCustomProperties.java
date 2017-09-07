package com.armedia.caliente.engine.ucm;

public enum UcmCustomProperties {
	//
	//
	;

	public final String name;

	private UcmCustomProperties() {
		this.name = name().toLowerCase();
	}
}