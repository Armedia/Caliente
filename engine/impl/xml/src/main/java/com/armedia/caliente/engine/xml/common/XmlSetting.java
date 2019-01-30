package com.armedia.caliente.engine.xml.common;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;

public enum XmlSetting implements TransferEngineSetting {
	//
	ROOT(CmfValue.Type.STRING),
	DB(CmfValue.Type.STRING),
	CONTENT(CmfValue.Type.STRING),
	AGGREGATE_FOLDERS(CmfValue.Type.BOOLEAN, false),
	AGGREGATE_DOCUMENTS(CmfValue.Type.BOOLEAN, false),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private XmlSetting(CmfValue.Type type) {
		this(type, null);
	}

	private XmlSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private XmlSetting(CmfValue.Type type, Object defaultValue, boolean required) {
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
	public CmfValue.Type getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}