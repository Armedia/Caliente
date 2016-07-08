package com.armedia.cmf.engine.alfresco.bulk.common;

public enum AlfProperties {
	//
	TARGET_PATHS,
	TARGET_PARENTS,
	CONTENTS,
	CURRENT_VERSION,
	//
	;

	public final String name;

	private AlfProperties() {
		this.name = name().toLowerCase();
	}
}