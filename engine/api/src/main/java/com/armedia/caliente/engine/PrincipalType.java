
package com.armedia.caliente.engine;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.caliente.store.CmfValueMapper.Mapping;
import com.armedia.commons.utilities.ConfigurationSetting;

public enum PrincipalType {

	//
	USER(CmfObject.Archetype.USER, TransferSetting.USER_MAP), //
	GROUP(CmfObject.Archetype.GROUP, TransferSetting.GROUP_MAP), //
	//
	;

	private final CmfObject.Archetype objectType;
	private final ConfigurationSetting setting;
	private final String mappingName;
	private final String defaultMappingFile;

	private PrincipalType(CmfObject.Archetype objectType, ConfigurationSetting setting) {
		this.objectType = objectType;
		this.setting = setting;
		this.mappingName = String.format("$%s_NAME$", name()).toUpperCase();
		this.defaultMappingFile = String.format("%smap.xml", name()).toLowerCase();
	}

	public final String mapName(CmfValueMapper mapper, String name) {
		Mapping m = mapper.getTargetMapping(this.objectType, this.mappingName, name);
		if (m == null) { return null; }
		String v = m.getTargetValue();
		if (StringUtils.isBlank(v)) {
			// Make sure we don't return blank values
			v = null;
		}
		return v;
	}

	public CmfObject.Archetype getObjectType() {
		return this.objectType;
	}

	public final String getMappingName() {
		return this.mappingName;
	}

	public final String getDefaultMappingFile() {
		return this.defaultMappingFile;
	}

	public final ConfigurationSetting getSetting() {
		return this.setting;
	}
}