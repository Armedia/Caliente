package com.armedia.caliente.engine.xml.common;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValueType;

public enum XmlSetting implements TransferEngineSetting {
	//
	ROOT(CmfValueType.STRING),
	DB(CmfValueType.STRING),
	CONTENT(CmfValueType.STRING),
	AGGREGATE_FOLDERS(CmfValueType.BOOLEAN, false),
	AGGREGATE_DOCUMENTS(CmfValueType.BOOLEAN, false),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValueType type;
	private final boolean required;

	private XmlSetting(CmfValueType type) {
		this(type, null);
	}

	private XmlSetting(CmfValueType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private XmlSetting(CmfValueType type, Object defaultValue, boolean required) {
		this.label = name().toLowerCase();
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