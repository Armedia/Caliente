package com.armedia.cmf.storage;

import com.armedia.cmf.storage.CmfDataType;

public interface CmfSetting {

	String getName();

	CmfDataType getType();

	boolean isRepeating();

}