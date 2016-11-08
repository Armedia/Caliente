package com.armedia.caliente.engine;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.ConfigurationSetting;

public interface TransferEngineSetting extends ConfigurationSetting {

	public CmfDataType getType();

	public boolean isRequired();
}