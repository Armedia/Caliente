package com.armedia.caliente.engine.local.common;

public enum LocalAttributes {
	//
	//
	;

	public final String name;

	private LocalAttributes() {
		this.name = name().toLowerCase();
	}
}