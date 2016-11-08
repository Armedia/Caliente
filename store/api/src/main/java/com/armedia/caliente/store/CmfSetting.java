package com.armedia.caliente.store;

import com.armedia.caliente.store.CmfDataType;

public interface CmfSetting {

	String getName();

	CmfDataType getType();

	boolean isRepeating();

}