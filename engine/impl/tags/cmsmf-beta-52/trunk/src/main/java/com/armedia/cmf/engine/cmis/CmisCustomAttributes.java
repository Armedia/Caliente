package com.armedia.cmf.engine.cmis;

public enum CmisCustomAttributes {
	//
	VERSION_ANTECEDENT_ID,
	ACL_OWNER,
	//
	;

	public final String name;

	private CmisCustomAttributes() {
		this.name = name().toLowerCase();
	}
}