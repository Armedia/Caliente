package com.armedia.caliente.engine.transform;

import java.util.Set;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

public interface TransformationContext<V> {

	public CmfObject<V> getObject();

	public String getCurrentSubtype();

	public void setCurrentSubtype();

	public Set<String> getCurrentDecorators();

	public CmfProperty<CmfValue> createVariable(String name, CmfDataType type);

	public CmfProperty<CmfValue> createVariable(String name, CmfDataType type, boolean repeating);

	public boolean hasVariable(String name);

	public void removeVariable(String name);

	public CmfProperty<CmfValue> getVariable(String name);

	public Set<String> getVariableNames();

}