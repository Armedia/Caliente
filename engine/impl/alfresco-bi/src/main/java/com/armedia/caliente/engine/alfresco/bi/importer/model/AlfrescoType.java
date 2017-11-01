package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public class AlfrescoType {

	private final SchemaMember<?> type;
	private final String name;
	private final Set<String> aspects;
	private final Map<String, SchemaMember<?>> attributes;
	private final Map<String, Set<String>> strippedAttributeNames;

	public static String stripNamespace(String attName) {
		if (attName == null) { return null; }
		return attName.replaceAll("^\\w+:", "");
	}

	AlfrescoType(SchemaMember<?> type, Collection<SchemaMember<?>> appliedAspects) {
		this.type = type;
		if (appliedAspects == null) {
			appliedAspects = Collections.emptyList();
		}
		this.name = type.name;
		Set<String> aspects = new LinkedHashSet<>(type.mandatoryAspects);

		Map<String, SchemaMember<?>> attributes = new TreeMap<>();
		for (SchemaMember<?> aspect : appliedAspects) {
			aspects.add(aspect.name);
			for (String attribute : aspect.getAllAttributeNames()) {
				attributes.put(attribute, aspect);
			}
		}
		// Override any aspect's attributes with our own
		for (String attribute : type.getAllAttributeNames()) {
			attributes.put(attribute, type);
		}
		this.attributes = Tools.freezeMap(new LinkedHashMap<>(attributes));

		Map<String, Set<String>> strippedAttributeNames = new TreeMap<>();
		for (String attName : this.attributes.keySet()) {
			// Remove the namespace
			String stripped = AlfrescoType.stripNamespace(attName);
			Set<String> s = strippedAttributeNames.get(stripped);
			if (s == null) {
				s = new TreeSet<>();
				strippedAttributeNames.put(stripped, s);
			}
			s.add(attName);
		}

		for (String stripped : new TreeSet<>(strippedAttributeNames.keySet())) {
			Set<String> s = strippedAttributeNames.get(stripped);
			s = Tools.freezeSet(s);
			strippedAttributeNames.put(stripped, s);
		}
		this.strippedAttributeNames = Tools.freezeMap(strippedAttributeNames);
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

	public boolean hasStrippedAttribute(String name) {
		return this.strippedAttributeNames.containsKey(name);
	}

	public Set<String> getStrippedAttributeMatches(String name) {
		return this.strippedAttributeNames.get(name);
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