package com.armedia.caliente.engine.ucm;

public enum UcmCustomAttributes {
	//
	VERSION_ANTECEDENT_ID, ACL_OWNER,
	//
	;

	public final String name;

	private UcmCustomAttributes() {
		this.name = name().toLowerCase();
	}
}