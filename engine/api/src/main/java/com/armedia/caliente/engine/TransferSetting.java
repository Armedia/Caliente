package com.armedia.caliente.engine;

import com.armedia.caliente.store.CmfValueType;

public enum TransferSetting implements TransferEngineSetting {
	//
	EXCLUDE_TYPES(CmfValueType.BOOLEAN),
	IGNORE_CONTENT(CmfValueType.BOOLEAN, false),
	THREAD_COUNT(CmfValueType.INTEGER),
	BACKLOG_SIZE(CmfValueType.INTEGER),
	LATEST_ONLY(CmfValueType.BOOLEAN, false),
	NO_RENDITIONS(CmfValueType.BOOLEAN, false),
	RETRY_ATTEMPTS(CmfValueType.INTEGER, 2),
	TRANSFORMATION(CmfValueType.STRING),
	FILTER(CmfValueType.STRING),
	EXTERNAL_METADATA(CmfValueType.STRING),
	USER_MAP(CmfValueType.STRING) {
		@Override
		public Object getDefaultValue() {
			return PrincipalType.USER.getDefaultMappingFile();
		}
	},
	GROUP_MAP(CmfValueType.STRING) {
		@Override
		public Object getDefaultValue() {
			return PrincipalType.GROUP.getDefaultMappingFile();
		}
	},
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValueType type;
	private final boolean required;

	private TransferSetting(CmfValueType type) {
		this(type, null);
	}

	private TransferSetting(CmfValueType type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private TransferSetting(CmfValueType type, Object defaultValue, boolean required) {
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
	public CmfValueType getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}