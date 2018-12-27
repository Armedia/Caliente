package com.armedia.caliente.engine.dynamic.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

public class AttributeMappingResult {

	private final Map<String, AttributeValue> values;
	private final boolean residualsEnabled;

	AttributeMappingResult(Map<String, AttributeValue> values, boolean residualsEnabled) {
		this.values = Tools.freezeMap(new LinkedHashMap<>(values));
		this.residualsEnabled = residualsEnabled;
	}

	public Set<String> getAttributeNames() {
		return this.values.keySet();
	}

	public AttributeValue getAttributeValue(String name) {
		if (name == null) { return null; }
		return this.values.get(name);
	}

	public boolean hasAttributeValue(String name) {
		if (name == null) { return false; }
		return this.values.containsKey(name);
	}

	public boolean isResidualsEnabled() {
		return this.residualsEnabled;
	}
}