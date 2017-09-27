package com.armedia.caliente.engine.transform;

import java.util.Set;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public interface TransformationContext<V> {

	public CmfObject<V> getObject();

	public String getObjectId();

	public String getHistoryId();

	public boolean isHistoryCurrent();

	public CmfType getType();

	public String getOriginalSubtype();

	public String getSubtype();

	public void setSubtype();

	public Set<String> getCurrentDecorators();

	public int getDependencyTier();

	public String getName();

	public void setName(String name);

	public String getProductName();

	public String getProductVersion();

	public Set<String> getAttributeNames();

	public boolean hasAttribute(CmfEncodeableName name);

	public boolean hasAttribute(String name);

	public CmfAttribute<CmfValue> getAttribute(CmfEncodeableName name);

	public CmfAttribute<CmfValue> getAttribute(String name);

	public CmfAttribute<CmfValue> setAttribute(CmfEncodeableName name, CmfDataType type);

	public CmfAttribute<CmfValue> setAttribute(String name, CmfDataType type);

	public CmfAttribute<CmfValue> setAttribute(CmfEncodeableName name, CmfDataType type, boolean multivalue);

	public CmfAttribute<CmfValue> setAttribute(String name, CmfDataType type, boolean multivalue);

	public CmfAttribute<CmfValue> removeAttribute(CmfEncodeableName name);

	public CmfAttribute<CmfValue> removeAttribute(String name);

	public Set<String> getPropertyNames();

	public boolean hasProperty(CmfEncodeableName name);

	public boolean hasProperty(String name);

	public CmfProperty<CmfValue> getProperty(CmfEncodeableName name);

	public CmfProperty<CmfValue> getProperty(String name);

	public CmfProperty<CmfValue> setProperty(CmfEncodeableName name, CmfDataType type);

	public CmfProperty<CmfValue> setProperty(String name, CmfDataType type);

	public CmfProperty<CmfValue> setProperty(CmfEncodeableName name, CmfDataType type, boolean multivalue);

	public CmfProperty<CmfValue> setProperty(String name, CmfDataType type, boolean multivalue);

	public CmfProperty<CmfValue> removeProperty(CmfEncodeableName name);

	public CmfProperty<CmfValue> removeProperty(String name);

	public Set<String> getVariableNames();

	public boolean hasVariable(String name);

	public CmfProperty<CmfValue> createVariable(CmfEncodeableName name, CmfDataType type);

	public CmfProperty<CmfValue> createVariable(String name, CmfDataType type);

	public CmfProperty<CmfValue> createVariable(CmfEncodeableName name, CmfDataType type, boolean repeating);

	public CmfProperty<CmfValue> createVariable(String name, CmfDataType type, boolean repeating);

	public CmfProperty<CmfValue> getVariable(String name);

	public CmfProperty<CmfValue> removeVariable(String name);
}