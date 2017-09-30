package com.armedia.caliente.engine.transform;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualTreeBidiMap;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeMapper;
import com.armedia.caliente.store.CmfAttributeMapper.Mapping;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public class TestTransformationContext implements TransformationContext {

	private String objectId = null;
	private final String historyId = null;
	private boolean historyCurrent = false;
	private CmfType type = null;
	private String originalSubtype = null;
	private String subtype = null;
	private Set<String> originalDecorators = new HashSet<>();
	private Set<String> decorators = new HashSet<>();
	private int dependencyTier = 0;
	private String name = null;
	private String productName = null;
	private String productVersion = null;

	private final Map<String, CmfAttribute<CmfValue>> attributes = new TreeMap<>();
	private final Map<String, CmfProperty<CmfValue>> properties = new TreeMap<>();
	private final Map<String, CmfProperty<CmfValue>> variables = new TreeMap<>();

	private final CmfAttributeMapper mapper = new CmfAttributeMapper() {

		private final Map<CmfType, Map<String, BidiMap<String, String>>> mappings = new EnumMap<>(CmfType.class);

		private Map<String, BidiMap<String, String>> getMappingsForType(CmfType type) {
			Objects.requireNonNull(type, "Must provide a type to retrieve the mappings for");
			Map<String, BidiMap<String, String>> typeMappings = this.mappings.get(type);
			if (typeMappings == null) {
				typeMappings = new TreeMap<>();
				this.mappings.put(type, typeMappings);
			}
			return typeMappings;
		}

		private BidiMap<String, String> getNamedMappingsForType(CmfType type, String name) {
			Objects.requireNonNull(name, "Must provide a name for the mapping sought");
			Map<String, BidiMap<String, String>> typeMappings = getMappingsForType(type);
			BidiMap<String, String> namedMappings = typeMappings.get(name);
			if (namedMappings == null) {
				namedMappings = new DualTreeBidiMap<>();
				typeMappings.put(name, namedMappings);
			}
			return namedMappings;
		}

		@Override
		public Mapping getTargetMapping(CmfType objectType, String mappingName, String sourceValue) {
			BidiMap<String, String> mappings = getNamedMappingsForType(objectType, mappingName);
			if (!mappings.containsKey(sourceValue)) { return null; }
			return newMapping(objectType, mappingName, sourceValue, mappings.get(sourceValue));
		}

		@Override
		public Mapping getSourceMapping(CmfType objectType, String mappingName, String targetValue) {
			BidiMap<String, String> mappings = getNamedMappingsForType(objectType, mappingName).inverseBidiMap();
			if (!mappings.containsKey(targetValue)) { return null; }
			return newMapping(objectType, mappingName, mappings.get(targetValue), targetValue);
		}

		@Override
		public Map<String, String> getMappings(CmfType objectType, String mappingName) {
			return new TreeMap<>(getNamedMappingsForType(objectType, mappingName));
		}

		@Override
		public Set<String> getAvailableMappings(CmfType objectType) {
			return new TreeSet<>(getMappingsForType(objectType).keySet());
		}

		@Override
		public Map<CmfType, Set<String>> getAvailableMappings() {
			Map<CmfType, Set<String>> ret = new EnumMap<>(CmfType.class);
			for (CmfType t : this.mappings.keySet()) {
				Set<String> s = null;
				if (this.mappings.containsKey(t)) {
					s = getAvailableMappings(t);
				} else {
					s = new TreeSet<>();
				}
				ret.put(t, s);
			}
			return ret;
		}

		@Override
		protected Mapping createMapping(CmfType objectType, String mappingName, String sourceValue,
			String targetValue) {
			if ((sourceValue == null) || (targetValue == null)) {
				// This is a removal...
				if ((sourceValue == null) && (targetValue == null)) { throw new IllegalArgumentException(
					"Must provide either a source or target value to search by"); }
				BidiMap<String, String> m = getNamedMappingsForType(objectType, mappingName);
				String key = sourceValue;
				if (sourceValue == null) {
					// Can only search by target
					key = targetValue;
					m = m.inverseBidiMap();
				}
				m.remove(key);
				return null;
			}

			Mapping m = newMapping(objectType, mappingName, sourceValue, targetValue);
			getNamedMappingsForType(objectType, mappingName).put(sourceValue, targetValue);
			return m;
		}
	};

	@Override
	public String getObjectId() {
		return this.objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	@Override
	public boolean isHistoryCurrent() {
		return this.historyCurrent;
	}

	public void setHistoryCurrent(boolean historyCurrent) {
		this.historyCurrent = historyCurrent;
	}

	@Override
	public CmfType getType() {
		return this.type;
	}

	public void setType(CmfType type) {
		this.type = type;
	}

	@Override
	public String getOriginalSubtype() {
		return this.originalSubtype;
	}

	public void setOriginalSubtype(String originalSubtype) {
		this.originalSubtype = originalSubtype;
	}

	@Override
	public String getSubtype() {
		return this.subtype;
	}

	@Override
	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	@Override
	public Set<String> getOriginalDecorators() {
		return this.originalDecorators;
	}

	public void setOriginalDecorators(Set<String> originalDecorators) {
		this.originalDecorators = originalDecorators;
	}

	@Override
	public Set<String> getDecorators() {
		return this.decorators;
	}

	public void setDecorators(Set<String> decorators) {
		this.decorators = decorators;
	}

	@Override
	public int getDependencyTier() {
		return this.dependencyTier;
	}

	public void setDependencyTier(int dependencyTier) {
		this.dependencyTier = dependencyTier;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getProductName() {
		return this.productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	@Override
	public String getProductVersion() {
		return this.productVersion;
	}

	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}

	@Override
	public String getHistoryId() {
		return this.historyId;
	}

	private Set<String> getNames(Map<String, ? extends CmfProperty<CmfValue>> map) {
		return new TreeSet<>(map.keySet());
	}

	private boolean hasName(String name, Map<String, ? extends CmfProperty<CmfValue>> map) {
		Objects.requireNonNull(name, "Must provide a name to seek");
		Objects.requireNonNull(map, "Must provide a map to seek from");
		return map.containsKey(name);
	}

	private <P extends CmfProperty<CmfValue>> P get(String name, Map<String, P> map) {
		Objects.requireNonNull(name, "Must provide a name to seek");
		Objects.requireNonNull(map, "Must provide a map to seek from");
		return map.get(name);
	}

	private <P extends CmfProperty<CmfValue>> P put(P value, Map<String, P> map) {
		Objects.requireNonNull(value, "Must provide a value to store");
		Objects.requireNonNull(map, "Must provide a map to store to");
		map.put(value.getName(), value);
		return value;
	}

	private <P extends CmfProperty<CmfValue>> P remove(String name, Map<String, P> map) {
		Objects.requireNonNull(name, "Must provide a name to remove");
		Objects.requireNonNull(map, "Must provide a map to remove from");
		return map.remove(name);
	}

	@Override
	public Set<String> getAttributeNames() {
		return getNames(this.attributes);
	}

	@Override
	public boolean hasAttribute(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to encode");
		return hasAttribute(name.encode());
	}

	@Override
	public boolean hasAttribute(String name) {
		return hasName(name, this.attributes);
	}

	@Override
	public CmfAttribute<CmfValue> getAttribute(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to encode");
		return getAttribute(name.encode());
	}

	@Override
	public CmfAttribute<CmfValue> getAttribute(String name) {
		return get(name, this.attributes);
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
		put(attribute, this.attributes);
		return attribute;
	}

	@Override
	public CmfAttribute<CmfValue> removeAttribute(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to remove");
		return removeAttribute(name.encode());
	}

	@Override
	public CmfAttribute<CmfValue> removeAttribute(String name) {
		return remove(name, this.attributes);
	}

	@Override
	public Set<String> getCalientePropertyNames() {
		return getNames(this.properties);

	}

	@Override
	public boolean hasCalienteProperty(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to encode");
		return hasCalienteProperty(name.encode());
	}

	@Override
	public boolean hasCalienteProperty(String name) {
		return hasName(name, this.properties);
	}

	@Override
	public CmfProperty<CmfValue> getCalienteProperty(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to encode");
		return getCalienteProperty(name.encode());
	}

	@Override
	public CmfProperty<CmfValue> getCalienteProperty(String name) {
		return get(name, this.properties);
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
		put(attribute, this.properties);
		return attribute;
	}

	@Override
	public CmfProperty<CmfValue> removeCalienteProperty(CmfEncodeableName name) {
		Objects.requireNonNull(name, "Must provide a name to remove");
		return removeCalienteProperty(name.encode());
	}

	@Override
	public CmfProperty<CmfValue> removeCalienteProperty(String name) {
		return remove(name, this.properties);
	}

	@Override
	public Set<String> getVariableNames() {
		return getNames(this.variables);
	}

	@Override
	public boolean hasVariable(String name) {
		return hasName(name, this.variables);
	}

	@Override
	public CmfProperty<CmfValue> getVariable(String name) {
		return get(name, this.variables);
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
		Objects.requireNonNull(name, "Must provide a name to create");
		return setVariable(name.encode(), type, multivalue);
	}

	@Override
	public CmfProperty<CmfValue> setVariable(String name, CmfDataType type, boolean multivalue) {
		CmfProperty<CmfValue> attribute = new CmfProperty<>(name, type, multivalue);
		put(attribute, this.variables);
		return attribute;
	}

	@Override
	public CmfProperty<CmfValue> removeVariable(String name) {
		return remove(name, this.variables);
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