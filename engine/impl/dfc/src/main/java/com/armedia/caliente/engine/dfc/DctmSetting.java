package com.armedia.caliente.engine.dfc;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.dfc.pool.DfcSessionFactory;

public enum DctmSetting implements TransferEngineSetting {
	//
	DOCBASE(DfcSessionFactory.DOCBASE, CmfValue.Type.STRING),
	USERNAME(DfcSessionFactory.USERNAME, CmfValue.Type.STRING),
	PASSWORD(DfcSessionFactory.PASSWORD, CmfValue.Type.STRING)
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private DctmSetting(CmfValue.Type type) {
		this(null, type, null);
	}

	private DctmSetting(String label, CmfValue.Type type) {
		this(label, type, null);
	}

	private DctmSetting(CmfValue.Type type, Object defaultValue) {
		this(null, type, defaultValue, false);
	}

	private DctmSetting(String label, CmfValue.Type type, Object defaultValue) {
		this(label, type, defaultValue, false);
	}

	private DctmSetting(CmfValue.Type type, Object defaultValue, boolean required) {
		this(null, type, defaultValue, false);
	}

	private DctmSetting(String label, CmfValue.Type type, Object defaultValue, boolean required) {
		this.label = (label != null ? label : name().toLowerCase());
		this.defaultValue = defaultValue;
		this.type = type;
		this.required = required;
	}

	@Override
	public final String getLabel() {
		return this.label;
	}

	@Override
	public final Object getDefaultValue() {
		return this.defaultValue;
	}

	@Override
	public CmfValue.Type getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}