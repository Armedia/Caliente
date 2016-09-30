package com.armedia.cmf.engine.xml.common;

import com.armedia.cmf.engine.TransferEngineSetting;
import com.armedia.cmf.storage.CmfDataType;

public enum XmlSetting implements TransferEngineSetting {
	//
	ROOT(CmfDataType.STRING),
	DB(CmfDataType.STRING),
	CONTENT(CmfDataType.STRING),
	AGGREGATE_FOLDERS(CmfDataType.BOOLEAN, false),
	AGGREGATE_DOCUMENTS(CmfDataType.BOOLEAN, false),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfDataType type;
	private final boolean required;

	private XmlSetting(CmfDataType type) {
		this(type, null);
	}

	private XmlSetting(CmfDataType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private XmlSetting(CmfDataType type, Object defaultValue, boolean required) {
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
	public CmfDataType getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}