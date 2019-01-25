package com.armedia.caliente.engine.transform;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualTreeBidiMap;

import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValueMapper;

public class TestAttributeMapper extends CmfValueMapper {

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
	public Collection<Mapping> getSourceMapping(CmfType objectType, String mappingName, String targetValue) {
		BidiMap<String, String> mappings = getNamedMappingsForType(objectType, mappingName).inverseBidiMap();
		if (!mappings.containsKey(targetValue)) { return null; }
		return Collections.singleton(newMapping(objectType, mappingName, mappings.get(targetValue), targetValue));
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
	protected Mapping createMapping(CmfType objectType, String mappingName, String sourceValue, String targetValue) {
		if ((sourceValue == null) || (targetValue == null)) {
			// This is a removal...
			if ((sourceValue == null) && (targetValue == null)) {
				throw new IllegalArgumentException("Must provide either a source or target value to search by");
			}
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
}