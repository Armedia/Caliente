package com.armedia.caliente.engine.transform;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeMapper;
import com.armedia.caliente.store.CmfAttributeMapper.Mapping;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class DefaultTransformationContext implements TransformationContext {

	private final CmfObject<CmfValue> object;
	private final CmfAttributeMapper mapper;

	private final Set<String> originalDecorators;
	private final Set<String> decorators;

	private final Map<String, CmfProperty<CmfValue>> variables = new TreeMap<>();

	private String subtype = null;
	private String name = null;

	public DefaultTransformationContext(CmfObject<CmfValue> object, CmfAttributeMapper mapper) {
		this.object = object;
		this.mapper = mapper;
		// TODO: Calculate the actual decorators associated with the object...
		this.originalDecorators = new LinkedHashSet<>();
		this.decorators = new LinkedHashSet<>();
	}

	@Override
	public String getObjectId() {
		return this.object.getId();
	}

	@Override
	public String getHistoryId() {
		return this.object.getHistoryId();
	}

	@Override
	public boolean isHistoryCurrent() {
		return this.object.isHistoryCurrent();
	}

	@Override
	public CmfType getType() {
		return this.object.getType();
	}

	@Override
	public String getOriginalSubtype() {
		return this.object.getSubtype();
	}

	@Override
	public String getSubtype() {
		return Tools.coalesce(this.subtype, getOriginalSubtype());
	}

	@Override
	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	@Override
	public Set<String> getOriginalDecorators() {
		return this.originalDecorators;
	}

	@Override
	public Set<String> getDecorators() {
		return this.decorators;
	}

	@Override
	public int getDependencyTier() {
		return this.object.getDependencyTier();
	}

	@Override
	public String getName() {
		return Tools.coalesce(this.name, this.object.getName());
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getProductName() {
		return this.object.getProductName();
	}

	@Override
	public String getProductVersion() {
		return this.object.getProductVersion();
	}

	@Override
	public Set<String> getAttributeNames() {
		return this.object.getAttributeNames();
	}

	@Override
	public boolean hasAttribute(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to encode");
		return hasAttribute(name.encode());
	}

	@Override
	public boolean hasAttribute(String name) {
		return getAttributeNames().contains(name);
	}

	@Override
	public CmfAttribute<CmfValue> getAttribute(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to encode");
		return getAttribute(name.encode());
	}

	@Override
	public CmfAttribute<CmfValue> getAttribute(String name) {
		return this.object.getAttribute(name);
	}

	@Override
	public CmfAttribute<CmfValue> setAttribute(CmfEncodeableName name, CmfDataType type) {
		return setAttribute(name, type, false);
	}

	@Override
	public CmfAttribute<CmfValue> setAttribute(String name, CmfDataType type) {
		return setAttribute(name, type, false);
	}

	@Override
	public CmfAttribute<CmfValue> setAttribute(CmfEncodeableName name, CmfDataType type, boolean multivalue) {
		Objects.requireNonNull(name, "Must provide a name to create");
		return setAttribute(name.encode(), type, multivalue);
	}

	@Override
	public CmfAttribute<CmfValue> setAttribute(String name, CmfDataType type, boolean multivalue) {
		CmfAttribute<CmfValue> attribute = new CmfAttribute<>(name, type, multivalue);
		this.object.setAttribute(attribute);
		return attribute;
	}

	@Override
	public CmfAttribute<CmfValue> removeAttribute(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to remove");
		return removeAttribute(name.encode());
	}

	@Override
	public CmfAttribute<CmfValue> removeAttribute(String name) {
		return this.object.removeAttribute(name);
	}

	@Override
	public Set<String> getCalientePropertyNames() {
		return this.object.getPropertyNames();
	}

	@Override
	public boolean hasCalienteProperty(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to encode");
		return hasCalienteProperty(name.encode());
	}

	@Override
	public boolean hasCalienteProperty(String name) {
		return getCalientePropertyNames().contains(name);
	}

	@Override
	public CmfProperty<CmfValue> getCalienteProperty(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to encode");
		return getCalienteProperty(name.encode());
	}

	@Override
	public CmfProperty<CmfValue> getCalienteProperty(String name) {
		return this.object.getProperty(name);
	}

	@Override
	public CmfProperty<CmfValue> setCalienteProperty(CmfEncodeableName name, CmfDataType type) {
		return setCalienteProperty(name, type, false);
	}

	@Override
	public CmfProperty<CmfValue> setCalienteProperty(String name, CmfDataType type) {
		return setCalienteProperty(name, type, false);
	}

	@Override
	public CmfProperty<CmfValue> setCalienteProperty(CmfEncodeableName name, CmfDataType type, boolean multivalue) {
		Objects.requireNonNull(name, "Must provide a name to create");
		return setCalienteProperty(name.encode(), type, multivalue);
	}

	@Override
	public CmfProperty<CmfValue> setCalienteProperty(String name, CmfDataType type, boolean multivalue) {
		CmfProperty<CmfValue> attribute = new CmfProperty<>(name, type, multivalue);
		this.object.setProperty(attribute);
		return attribute;
	}

	@Override
	public CmfProperty<CmfValue> removeCalienteProperty(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to remove");
		return removeCalienteProperty(name.encode());
	}

	@Override
	public CmfProperty<CmfValue> removeCalienteProperty(String name) {
		return this.object.removeProperty(name);
	}

	@Override
	public Set<String> getVariableNames() {
		return new TreeSet<>(this.variables.keySet());
	}

	@Override
	public boolean hasVariable(String name) {
		Objects.requireNonNull(name, "Must provide a variable name to search for");
		return this.variables.containsKey(name);
	}

	@Override
	public CmfProperty<CmfValue> setVariable(CmfEncodeableName name, CmfDataType type) {
		return setVariable(name, type, false);
	}

	@Override
	public CmfProperty<CmfValue> setVariable(String name, CmfDataType type) {
		return setVariable(name, type, false);
	}

	@Override
	public CmfProperty<CmfValue> setVariable(CmfEncodeableName name, CmfDataType type, boolean multivalue) {
		Objects.requireNonNull(name, "Must provide a variable name to store");
		return setVariable(name.encode(), type, multivalue);
	}

	@Override
	public CmfProperty<CmfValue> setVariable(String name, CmfDataType type, boolean multivalue) {
		Objects.requireNonNull(name, "Must provide a variable name to store");
		Objects.requireNonNull(type, "Must provide a data type for the variable value");
		CmfProperty<CmfValue> variable = new CmfProperty<>(name, type, multivalue);
		this.variables.put(name, variable);
		return variable;
	}

	@Override
	public CmfProperty<CmfValue> getVariable(String name) {
		Objects.requireNonNull(name, "Must provide a variable name to seek");
		return this.variables.get(name);
	}

	@Override
	public CmfProperty<CmfValue> removeVariable(String name) {
		Objects.requireNonNull(name, "Must provide a variable name to remove");
		return this.variables.remove(name);
	}

	@Override
	public void setMapping(CmfType objectType, String mappingName, String sourceValue, String targetValue) {
		this.mapper.setMapping(objectType, mappingName, sourceValue, targetValue);
	}

	@Override
	public boolean hasTargetMapping(CmfType objectType, String mappingName, String sourceValue) {
		return (getTargetMapping(objectType, mappingName, sourceValue) != null);
	}

	@Override
	public void clearTargetMapping(CmfType objectType, String mappingName, String sourceValue) {
		this.mapper.clearTargetMapping(objectType, mappingName, sourceValue);
	}

	@Override
	public Mapping getTargetMapping(CmfType objectType, String mappingName, String sourceValue) {
		return this.mapper.getTargetMapping(objectType, mappingName, sourceValue);
	}

	@Override
	public boolean hasSourceMapping(CmfType objectType, String mappingName, String targetValue) {
		return (getSourceMapping(objectType, mappingName, targetValue) != null);
	}

	@Override
	public void clearSourceMapping(CmfType objectType, String mappingName, String targetValue) {
		this.mapper.clearSourceMapping(objectType, mappingName, targetValue);
	}

	@Override
	public Mapping getSourceMapping(CmfType objectType, String mappingName, String targetValue) {
		return this.mapper.getSourceMapping(objectType, mappingName, targetValue);
	}
}