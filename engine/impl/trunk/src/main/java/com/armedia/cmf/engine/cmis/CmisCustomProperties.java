package com.armedia.cmf.engine.cmis;

public enum CmisCustomProperties {
	//
	TARGET_PATHS,
	TARGET_PARENTS,
	CONTENTS,
	//
	;

	public final String name;

	private CmisCustomProperties() {
		this.name = name().toLowerCase();
	}
}