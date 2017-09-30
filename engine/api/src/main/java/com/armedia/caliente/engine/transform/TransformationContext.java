package com.armedia.caliente.engine.transform;

import java.util.Set;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeMapper.Mapping;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public interface TransformationContext {

	public String getObjectId();

	public String getHistoryId();

	public boolean isHistoryCurrent();

	public CmfType getType();

	public String getOriginalSubtype();

	public String getSubtype();

	public void setSubtype(String subtype);

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

	public Set<String> getCalientePropertyNames();

	public boolean hasCalienteProperty(CmfEncodeableName name);

	public boolean hasCalienteProperty(String name);

	public CmfProperty<CmfValue> getCalienteProperty(CmfEncodeableName name);

	public CmfProperty<CmfValue> getCalienteProperty(String name);

	public CmfProperty<CmfValue> setCalienteProperty(CmfEncodeableName name, CmfDataType type);

	public CmfProperty<CmfValue> setCalienteProperty(String name, CmfDataType type);

	public CmfProperty<CmfValue> setCalienteProperty(CmfEncodeableName name, CmfDataType type, boolean multivalue);

	public CmfProperty<CmfValue> setCalienteProperty(String name, CmfDataType type, boolean multivalue);

	public CmfProperty<CmfValue> removeCalienteProperty(CmfEncodeableName name);

	public CmfProperty<CmfValue> removeCalienteProperty(String name);

	public Set<String> getVariableNames();

	public boolean hasVariable(String name);

	public CmfProperty<CmfValue> setVariable(CmfEncodeableName name, CmfDataType type);

	public CmfProperty<CmfValue> setVariable(String name, CmfDataType type);

	public CmfProperty<CmfValue> setVariable(CmfEncodeableName name, CmfDataType type, boolean repeating);

	public CmfProperty<CmfValue> setVariable(String name, CmfDataType type, boolean repeating);

	public CmfProperty<CmfValue> getVariable(String name);

	public CmfProperty<CmfValue> removeVariable(String name);

	public void setMapping(CmfType objectType, String mappingName, String sourceValue, String targetValue);

	public boolean hasTargetMapping(CmfType objectType, String mappingName, String sourceValue);

	public void clearTargetMapping(CmfType objectType, String mappingName, String sourceValue);

	public Mapping getTargetMapping(CmfType objectType, String mappingName, String sourceValue);

	public boolean hasSourceMapping(CmfType objectType, String mappingName, String targetValue);

	public void clearSourceMapping(CmfType objectType, String mappingName, String targetValue);

	public Mapping getSourceMapping(CmfType objectType, String mappingName, String targetValue);

}