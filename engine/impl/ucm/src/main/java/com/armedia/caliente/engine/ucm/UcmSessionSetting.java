package com.armedia.caliente.engine.ucm;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum UcmSessionSetting implements ConfigurationSetting {
	//
	USER, //
	PASSWORD, //
	SERVER, //
	PORT, //
	SSL_SERVER_CHAIN, //
	SSL_SERVER_CERT, //
	SSL_KEY, //
	SSL_CERTIFICATE, //
	SSL_CHAIN, //
	//
	;

	private final String label;
	private final Object defaultValue;

	private UcmSessionSetting() {
		this(null);
	}

	private UcmSessionSetting(Object defaultValue) {
		this.label = name().toLowerCase().replace('_', '.');
		this.defaultValue = defaultValue;
	}

	@Override
	public final String getLabel() {
		return this.label;
	}

	@Override
	public final Object getDefaultValue() {
		return this.defaultValue;
	}
}