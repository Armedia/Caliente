package com.armedia.cmf.engine.sharepoint;

public enum ShptProperties {

	//
	USERS_WITH_DEFAULT_GROUP,
	TARGET_PATHS,
	TARGET_PARENTS,
	VERSION_PATCHES,
	PATCH_ANTECEDENT,
	USERS_WITH_DEFAULT_FOLDER,
	USERS_DEFAULT_FOLDER_PATHS,
	CONTENTS,
	CURRENT_VERSION,
	//
	;

	public final String name;

	private ShptProperties() {
		this.name = name().toLowerCase();
	}
}