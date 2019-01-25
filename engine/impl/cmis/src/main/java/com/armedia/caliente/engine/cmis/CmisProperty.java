package com.armedia.caliente.engine.cmis;

import java.util.Map;
import java.util.function.Supplier;

import com.armedia.caliente.engine.converter.MappingManager;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.commons.utilities.Tools;

public enum CmisProperty implements Supplier<String>, CmfEncodeableName {

	//
	PRODUCT_NAME(CmfDataType.STRING), PRODUCT_VERSION(CmfDataType.STRING), ACL_PERMISSION(CmfDataType.STRING, true),
	//
	;

	public static final String PERMISSION_PROPERTY_FMT = "cmf:%s:aclPermission";

	public final String name;
	public final CmfDataType type;
	public final boolean repeating;

	private CmisProperty(CmfDataType type) {
		this(type, false);
	}

	private CmisProperty(CmfDataType type, boolean repeating) {
		this.name = String.format("cmis:%s", name().toLowerCase());
		this.type = type;
		this.repeating = repeating;
	}

	@Override
	public final String encode() {
		return this.name;
	}

	@Override
	public String get() {
		return this.name;
	}

	private static volatile Map<String, CmisProperty> MAPPINGS = null;

	private static void initMappings() {
		if (CmisProperty.MAPPINGS == null) {
			synchronized (CmisProperty.class) {
				if (CmisProperty.MAPPINGS == null) {
					CmisProperty.MAPPINGS = Tools
						.freezeMap(MappingManager.createMappings(CmisProperty.class, CmisProperty.values()));
				}
			}
		}
	}

	public static CmisProperty decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		CmisProperty.initMappings();
		CmisProperty ret = CmisProperty.MAPPINGS.get(name);
		if (ret == null) {
			throw new IllegalArgumentException(
				String.format("Failed to decode [%s] into a valid intermediate property", name));
		}
		return ret;
	}
}