package com.armedia.caliente.engine.dynamic.mapper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

public class AttributeMappingResult implements Iterable<AttributeMapping> {

	private final Map<String, AttributeMapping> mappings;
	private final boolean residualsEnabled;

	AttributeMappingResult(Map<String, AttributeMapping> mappings, boolean residualsEnabled) {
		this.mappings = Tools.freezeMap(new LinkedHashMap<>(mappings));
		this.residualsEnabled = residualsEnabled;
	}

	public Set<String> getAttributeNames() {
		return this.mappings.keySet();
	}

	public AttributeMapping getAttributeMapping(String name) {
		if (name == null) { return null; }
		return this.mappings.get(name);
	}

	public boolean hasAttributeValue(String name) {
		if (name == null) { return false; }
		return this.mappings.containsKey(name);
	}

	public boolean isResidualsEnabled() {
		return this.residualsEnabled;
	}

	@Override
	public Iterator<AttributeMapping> iterator() {
		return this.mappings.values().iterator();
	}
}