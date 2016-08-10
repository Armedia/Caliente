package com.armedia.cmf.engine.alfresco.bulk.importer.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.commons.utilities.Tools;

public class AlfrescoType {

	private final String name;
	private final Set<String> aspects;
	private final Map<String, SchemaMember<?>> attributes;

	AlfrescoType(SchemaMember<?> type, Collection<SchemaMember<?>> appliedAspects) {
		if (appliedAspects == null) {
			appliedAspects = Collections.emptyList();
		}
		this.name = type.name;
		Set<String> aspects = new LinkedHashSet<String>(type.mandatoryAspects);

		Map<String, SchemaMember<?>> attributes = new TreeMap<String, SchemaMember<?>>();
		for (String attribute : type.getAttributeNames()) {
			attributes.put(attribute, type);
		}
		for (SchemaMember<?> aspect : appliedAspects) {
			aspects.add(aspect.name);
			for (String attribute : aspect.getAttributeNames()) {
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
}