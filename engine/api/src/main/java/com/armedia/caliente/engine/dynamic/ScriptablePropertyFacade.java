package com.armedia.caliente.engine.dynamic;

import java.util.List;

import com.armedia.caliente.store.CmfValue;

public interface ScriptablePropertyFacade {

	public String getName();

	public CmfValue.Type getType();

	public boolean isMultivalued();

	public boolean isEmpty();

	public Object getValue();

	public List<Object> getValues();

	public int getSize();

	public default boolean isMutable() {
		return false;
	}
}