package com.armedia.cmf.engine;

import com.armedia.cmf.storage.CmfDataType;
import com.armedia.commons.utilities.ConfigurationSetting;

public interface TransferEngineSetting extends ConfigurationSetting {

	public CmfDataType getType();

	public boolean isRequired();
}