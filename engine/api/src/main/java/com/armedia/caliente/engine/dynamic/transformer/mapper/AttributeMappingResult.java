package com.armedia.caliente.engine.dynamic.transformer.mapper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.ConstructedType;
import com.armedia.commons.utilities.Tools;

public class AttributeMappingResult implements Iterable<AttributeMapping> {

	private final ConstructedType type;
	private final Map<String, AttributeMapping> mappings;
	private final boolean residualsEnabled;

	AttributeMappingResult(ConstructedType type, Map<String, AttributeMapping> mappings, boolean residualsEnabled) {
		this.type = type;
		this.mappings = Tools.freezeMap(new LinkedHashMap<>(mappings));
		this.residualsEnabled = residualsEnabled;
	}

	public final ConstructedType getType() {
		return this.type;
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