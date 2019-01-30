package com.armedia.caliente.engine.dfc;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValueType;
import com.armedia.commons.dfc.pool.DfcSessionFactory;

public enum DctmSetting implements TransferEngineSetting {
	//
	DOCBASE(DfcSessionFactory.DOCBASE, CmfValueType.STRING),
	USERNAME(DfcSessionFactory.USERNAME, CmfValueType.STRING),
	PASSWORD(DfcSessionFactory.PASSWORD, CmfValueType.STRING)
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValueType type;
	private final boolean required;

	private DctmSetting(CmfValueType type) {
		this(null, type, null);
	}

	private DctmSetting(String label, CmfValueType type) {
		this(label, type, null);
	}

	private DctmSetting(CmfValueType type, Object defaultValue) {
		this(null, type, defaultValue, false);
	}

	private DctmSetting(String label, CmfValueType type, Object defaultValue) {
		this(label, type, defaultValue, false);
	}

	private DctmSetting(CmfValueType type, Object defaultValue, boolean required) {
		this(null, type, defaultValue, false);
	}

	private DctmSetting(String label, CmfValueType type, Object defaultValue, boolean required) {
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
	public CmfValueType getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}