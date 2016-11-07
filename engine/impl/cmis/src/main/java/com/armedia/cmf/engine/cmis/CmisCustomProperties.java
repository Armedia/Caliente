package com.armedia.cmf.engine.cmis;

public enum CmisCustomProperties {
	//
	//
	;

	public final String name;

	private CmisCustomProperties() {
		this.name = name().toLowerCase();
	}
}