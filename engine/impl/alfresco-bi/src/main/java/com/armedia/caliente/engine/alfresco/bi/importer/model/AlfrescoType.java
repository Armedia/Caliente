package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoContentModel.Aspect;
import com.armedia.commons.utilities.Tools;

public class AlfrescoType {

	private final SchemaMember<?> type;
	private final String name;
	private final Map<String, Aspect> aspects;
	private final Map<String, SchemaMember<?>> attributes;
	private final Map<String, Set<String>> strippedAttributeNames;
	private final String signature;

	public static String stripNamespace(String attName) {
		if (attName == null) { return null; }
		return attName.replaceAll("^\\w+:", "");
	}

	AlfrescoType(SchemaMember<?> type, Collection<Aspect> appliedAspects) {
		this.type = type;
		if (appliedAspects == null) {
			appliedAspects = Collections.emptyList();
		}
		this.name = type.name;
		Map<String, Aspect> aspects = new LinkedHashMap<>(type.mandatoryAspects);

		Map<String, SchemaMember<?>> attributes = new TreeMap<>();
		for (Aspect aspect : appliedAspects) {
			aspects.put(aspect.name, aspect);
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
		this.aspects = Tools.freezeMap(aspects);

		Set<String> s = new TreeSet<>();
		s.addAll(type.getAncestors());
		s.add(type.name);
		s.addAll(this.aspects.keySet());
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (String str : s) {
			if (sb.length() > 1) {
				sb.append('|');
			}
			sb.append(str);
		}
		sb.append(']');
		this.signature = DigestUtils.sha256Hex(sb.toString());
	}

	public String getSignature() {
		return this.signature;
	}

	public String getName() {
		return this.name;
	}

	public boolean isDescendedOf(String typeName) {
		if (this.aspects.containsKey(typeName)) { return true; }
		if (this.type.isDescendedOf(typeName)) { return true; }
		for (String aspectName : this.aspects.keySet()) {
			if (this.aspects.get(aspectName).isDescendedOf(typeName)) { return true; }
		}
		return false;
	}

	public Set<String> getAspects() {
		return this.aspects.keySet();
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