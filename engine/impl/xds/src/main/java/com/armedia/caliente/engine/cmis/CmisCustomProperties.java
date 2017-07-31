package com.armedia.caliente.engine.cmis;

public enum CmisCustomProperties {
	//
	//
	;

	public final String name;

	private CmisCustomProperties() {
		this.name = name().toLowerCase();
	}
}