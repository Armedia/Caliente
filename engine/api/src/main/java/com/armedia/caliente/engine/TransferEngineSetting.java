package com.armedia.caliente.engine;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.ConfigurationSetting;

public interface TransferEngineSetting extends ConfigurationSetting {

	public CmfValue.Type getType();

	public boolean isRequired();
}