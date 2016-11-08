package com.armedia.caliente.engine.alfresco.bulk.importer.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.commons.utilities.Tools;

public class AlfrescoType {

	private final SchemaMember<?> type;
	private final String name;
	private final Set<String> aspects;
	private final Map<String, SchemaMember<?>> attributes;

	AlfrescoType(SchemaMember<?> type, Collection<SchemaMember<?>> appliedAspects) {
		this.type = type;
		if (appliedAspects == null) {
			appliedAspects = Collections.emptyList();
		}
		this.name = type.name;
		Set<String> aspects = new LinkedHashSet<String>(type.mandatoryAspects);

		Map<String, SchemaMember<?>> attributes = new TreeMap<String, SchemaMember<?>>();
		for (String attribute : type.getAllAttributeNames()) {
			attributes.put(attribute, type);
		}
		for (SchemaMember<?> aspect : appliedAspects) {
			aspects.add(aspect.name);
			for (String attribute : aspect.getAllAttributeNames()) {
				if (!attributes.containsKey(attribute)) {
					attributes.put(attribute, aspect);
				}
			}
		}
		this.attributes = new LinkedHashMap<String, SchemaMember<?>>();
		for (String att : attributes.keySet()) {
			this.attributes.put(att, attributes.get(att));
		}
		this.aspects = Tools.freezeSet(aspects);
	}

	public String getName() {
		return this.name;
	}

	public boolean isDescendedOf(String typeName) {
		return this.aspects.contains(typeName) || this.type.isDescendedOf(typeName);
	}

	public Set<String> getAspects() {
		return this.aspects;
	}

	public SchemaAttribute getAttribute(String name) {
		SchemaMember<?> m = this.attributes.get(name);
		if (m == null) { return null; }
		return m.getAttribute(name);
	}

	public Set<String> getAttributeNames() {
		return this.attributes.keySet();
	}

	public int getAttributeCount() {
		return this.attributes.size();
	}

	@Override
	public String toString() {
		return String.format("AlfrescoType [name=%s, aspects=%s]", this.name, this.aspects);
	}
}