package com.armedia.caliente.engine.ucm;

public enum CmisCustomProperties {
	//
	//
	;

	public final String name;

	private CmisCustomProperties() {
		this.name = name().toLowerCase();
	}
}