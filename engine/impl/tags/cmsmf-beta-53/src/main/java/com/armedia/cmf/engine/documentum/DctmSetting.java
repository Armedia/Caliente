package com.armedia.cmf.engine.documentum;

import com.armedia.cmf.engine.TransferEngineSetting;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.commons.dfc.pool.DfcSessionFactory;

public enum DctmSetting implements TransferEngineSetting {
	//
	DOCBASE(DfcSessionFactory.DOCBASE, CmfDataType.STRING),
	USERNAME(DfcSessionFactory.USERNAME, CmfDataType.STRING),
	PASSWORD(DfcSessionFactory.PASSWORD, CmfDataType.STRING)
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfDataType type;
	private final boolean required;

	private DctmSetting(CmfDataType type) {
		this(null, type, null);
	}

	private DctmSetting(String label, CmfDataType type) {
		this(label, type, null);
	}

	private DctmSetting(CmfDataType type, Object defaultValue) {
		this(null, type, defaultValue, false);
	}

	private DctmSetting(String label, CmfDataType type, Object defaultValue) {
		this(label, type, defaultValue, false);
	}

	private DctmSetting(CmfDataType type, Object defaultValue, boolean required) {
		this(null, type, defaultValue, false);
	}

	private DctmSetting(String label, CmfDataType type, Object defaultValue, boolean required) {
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
	public CmfDataType getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}