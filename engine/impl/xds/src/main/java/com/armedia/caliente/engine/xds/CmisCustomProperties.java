package com.armedia.caliente.engine.xds;

public enum CmisCustomProperties {
	//
	//
	;

	public final String name;

	private CmisCustomProperties() {
		this.name = name().toLowerCase();
	}
}