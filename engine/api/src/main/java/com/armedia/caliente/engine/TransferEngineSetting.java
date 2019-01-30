package com.armedia.caliente.engine;

import com.armedia.caliente.store.CmfValueType;
import com.armedia.commons.utilities.ConfigurationSetting;

public interface TransferEngineSetting extends ConfigurationSetting {

	public CmfValueType getType();

	public boolean isRequired();
}