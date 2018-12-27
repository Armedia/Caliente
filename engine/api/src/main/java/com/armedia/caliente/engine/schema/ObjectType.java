package com.armedia.caliente.engine.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;

import com.armedia.caliente.engine.schema.SchemaContentModel.Aspect;
import com.armedia.commons.utilities.Tools;

public class ObjectType {

	private final SchemaMember<?> type;
	private final String name;
	private final Map<String, Aspect> aspects;
	private final Map<String, Aspect> extraAspects;
	private final Set<String> declaredAspects;
	private final Map<String, SchemaMember<?>> attributes;
	private final String signature;

	public static String stripNamespace(String attName) {
		if (attName == null) { return null; }
		return attName.replaceAll("^\\w+:", "");
	}

	ObjectType(SchemaMember<?> type, Collection<Aspect> appliedAspects) {
		this.type = type;
		if (appliedAspects == null) {
			appliedAspects = Collections.emptyList();
		}
		this.name = type.name;
		Set<String> declaredAspects = new HashSet<>();
		if (type.parent != null) {
			declaredAspects.addAll(type.parent.getAllAspects());
		}
		declaredAspects.addAll(type.mandatoryAspects.keySet());
		Map<String, Aspect> aspects = new LinkedHashMap<>(type.mandatoryAspects);

		Map<String, SchemaMember<?>> attributes = new TreeMap<>();
		// Go through the parent's attributes
		if (type.parent != null) {
			for (String attribute : type.parent.getAllAttributeNames()) {
				attributes.put(attribute, type.parent.getAttribute(attribute).declaration);
			}
		}

		Map<String, Aspect> extraAspects = new TreeMap<>();
		for (Aspect aspect : appliedAspects) {
			aspects.put(aspect.name, aspect);
			if (!declaredAspects.contains(aspect.name)) {
				extraAspects.put(aspect.name, aspect);
			}
			for (String attribute : aspect.getAllAttributeNames()) {
				attributes.put(attribute, aspect);
			}
		}

		// Override any aspect's attributes with our own
		for (String attribute : type.getAllAttributeNames()) {
			attributes.put(attribute, type);
		}
		this.attributes = Tools.freezeMap(new LinkedHashMap<>(attributes));

		this.declaredAspects = Tools.freezeCopy(type.mandatoryAspects.keySet());
		this.extraAspects = Tools.freezeMap(extraAspects);
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

	public SchemaMember<?> getDeclaration() {
		return this.type;
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

	public Set<String> getExtraAspects() {
		return this.extraAspects.keySet();
	}

	public Set<String> getDeclaredAspects() {
		return this.declaredAspects;
	}

	public boolean isAttributeInherited(String name) {
		return this.type.isAttributeInherited(name);
	}

	public boolean isAspectInherited(String aspect) {
		return this.type.isAspectInherited(this.name);
	}

	public SchemaAttribute getAttribute(String name) {
		SchemaMember<?> m = this.attributes.get(name);
		if (m == null) { return null; }
		return m.getAttribute(name);
	}

	public boolean hasAttribute(String name) {
		return this.attributes.containsKey(name);
	}

	public Set<String> getAttributeNames() {
		return this.attributes.keySet();
	}

	public int getAttributeCount() {
		return this.attributes.size();
	}

	@Override
	public String toString() {
		return String.format("ObjectType [name=%s, aspects=%s]", this.name, this.aspects);
	}
}