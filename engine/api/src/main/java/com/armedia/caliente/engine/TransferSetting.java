package com.armedia.caliente.engine;

import com.armedia.caliente.store.CmfDataType;

public enum TransferSetting implements TransferEngineSetting {
	//
	EXCLUDE_TYPES(CmfDataType.BOOLEAN),
	IGNORE_CONTENT(CmfDataType.BOOLEAN, false),
	THREAD_COUNT(CmfDataType.INTEGER),
	BACKLOG_SIZE(CmfDataType.INTEGER),
	LATEST_ONLY(CmfDataType.BOOLEAN, false),
	NO_RENDITIONS(CmfDataType.BOOLEAN, false),
	RETRY_ATTEMPTS(CmfDataType.INTEGER, 2),
	TRANSFORMATION(CmfDataType.STRING),
	FILTER(CmfDataType.STRING),
	EXTERNAL_METADATA(CmfDataType.STRING),
	USER_MAP(CmfDataType.STRING) {
		@Override
		public Object getDefaultValue() {
			return PrincipalType.USER.getDefaultMappingFile();
		}
	},
	GROUP_MAP(CmfDataType.STRING) {
		@Override
		public Object getDefaultValue() {
			return PrincipalType.GROUP.getDefaultMappingFile();
		}
	},
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfDataType type;
	private final boolean required;

	private TransferSetting(CmfDataType type) {
		this(type, null);
	}

	private TransferSetting(CmfDataType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private TransferSetting(CmfDataType type, Object defaultValue, boolean required) {
		if (type == null) { throw new IllegalArgumentException("Must provide a data type"); }
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
	public Object getDefaultValue() {
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