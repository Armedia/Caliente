package com.armedia.caliente.store;

public interface CmfSetting {

	String getName();

	CmfValue.Type getType();

	boolean isRepeating();

}