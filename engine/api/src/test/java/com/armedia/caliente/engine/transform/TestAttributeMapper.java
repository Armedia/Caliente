/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValueMapper;

public class TestAttributeMapper extends CmfValueMapper {

	private final Map<CmfObject.Archetype, Map<String, BidiMap<String, String>>> mappings = new EnumMap<>(
		CmfObject.Archetype.class);

	private Map<String, BidiMap<String, String>> getMappingsForType(CmfObject.Archetype type) {
		Objects.requireNonNull(type, "Must provide a type to retrieve the mappings for");
		Map<String, BidiMap<String, String>> typeMappings = this.mappings.get(type);
		if (typeMappings == null) {
			typeMappings = new TreeMap<>();
			this.mappings.put(type, typeMappings);
		}
		return typeMappings;
	}

	private BidiMap<String, String> getNamedMappingsForType(CmfObject.Archetype type, String name) {
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
	public Mapping getTargetMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue) {
		BidiMap<String, String> mappings = getNamedMappingsForType(objectType, mappingName);
		if (!mappings.containsKey(sourceValue)) { return null; }
		return newMapping(objectType, mappingName, sourceValue, mappings.get(sourceValue));
	}

	@Override
	public Collection<Mapping> getSourceMapping(CmfObject.Archetype objectType, String mappingName,
		String targetValue) {
		BidiMap<String, String> mappings = getNamedMappingsForType(objectType, mappingName).inverseBidiMap();
		if (!mappings.containsKey(targetValue)) { return null; }
		return Collections.singleton(newMapping(objectType, mappingName, mappings.get(targetValue), targetValue));
	}

	@Override
	public Map<String, String> getMappings(CmfObject.Archetype objectType, String mappingName) {
		return new TreeMap<>(getNamedMappingsForType(objectType, mappingName));
	}

	@Override
	public Set<String> getAvailableMappings(CmfObject.Archetype objectType) {
		return new TreeSet<>(getMappingsForType(objectType).keySet());
	}

	@Override
	public Map<CmfObject.Archetype, Set<String>> getAvailableMappings() {
		Map<CmfObject.Archetype, Set<String>> ret = new EnumMap<>(CmfObject.Archetype.class);
		for (CmfObject.Archetype t : this.mappings.keySet()) {
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
	protected Mapping createMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue,
		String targetValue) {
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