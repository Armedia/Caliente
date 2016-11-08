package com.armedia.caliente.engine.local.common;

public enum LocalProperties {
	//
	TARGET_PATHS,
	TARGET_PARENTS,
	CONTENTS,
	CURRENT_VERSION,
	//
	;

	public final String name;

	private LocalProperties() {
		this.name = name().toLowerCase();
	}
}