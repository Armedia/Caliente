package com.armedia.caliente.engine;

import com.armedia.caliente.store.CmfValue;

public enum TransferSetting implements TransferEngineSetting {
	//
	EXCEPT_TYPES(CmfValue.Type.STRING),
	ONLY_TYPES(CmfValue.Type.STRING),
	IGNORE_CONTENT(CmfValue.Type.BOOLEAN, false),
	THREAD_COUNT(CmfValue.Type.INTEGER),
	BACKLOG_SIZE(CmfValue.Type.INTEGER),
	LATEST_ONLY(CmfValue.Type.BOOLEAN, false),
	NO_RENDITIONS(CmfValue.Type.BOOLEAN, false),
	RETRY_ATTEMPTS(CmfValue.Type.INTEGER, 2),
	TRANSFORMATION(CmfValue.Type.STRING),
	FILTER(CmfValue.Type.STRING),
	EXTERNAL_METADATA(CmfValue.Type.STRING),
	USER_MAP(CmfValue.Type.STRING) {
		@Override
		public Object getDefaultValue() {
			return PrincipalType.USER.getDefaultMappingFile();
		}
	},
	GROUP_MAP(CmfValue.Type.STRING) {
		@Override
		public Object getDefaultValue() {
			return PrincipalType.GROUP.getDefaultMappingFile();
		}
	},
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private TransferSetting(CmfValue.Type type) {
		this(type, null);
	}

	private TransferSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private TransferSetting(CmfValue.Type type, Object defaultValue, boolean required) {
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
	public CmfValue.Type getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}