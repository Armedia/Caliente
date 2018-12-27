package com.armedia.caliente.engine.importer.schema;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;

import com.armedia.caliente.engine.importer.schema.SchemaContentModel.Aspect;
import com.armedia.caliente.engine.importer.schema.decl.AttributeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.SchemaDeclarationService;
import com.armedia.commons.utilities.Tools;

public abstract class SchemaMember<T extends SchemaMember<T>> {

	protected final T parent;
	protected final String name;

	protected final Map<String, T> ancestors;
	protected final Map<String, SecondaryType> secondaries;
	protected final Map<String, SecondaryType> allSecondaries;
	protected final Map<String, ObjectAttribute> attributes;
	protected final Map<String, ObjectAttribute> allAttributes;
	protected final Set<String> allAttributeNames;
	protected final String signature;

	SchemaMember(SchemaDeclarationService schemaService, String name, Collection<String> secondaries, T parent) {
		this.parent = parent;
		this.name = name;

		Map<String, ObjectAttribute> localAttributes = new TreeMap<>();
		for (AttributeDeclaration property : schemaService.getAttributes(name)) {
			String propertyName = property.name;
			// Is this a duplicate at this level?
			if (localAttributes.containsKey(propertyName)) { throw new IllegalStateException(
				String.format("Duplicate attribute name [%s] on type [%s]", propertyName, this.name)); }

			// Is this a duplicate at my parent's level?
			if ((parent != null) && (parent.getAttribute(propertyName) != null)) { throw new IllegalStateException(
				String.format("Duplicate attribute name [%s] on type [%s] - already defined by a supertype",
					propertyName, this.name)); }

			// No dupes, add the attribute
			ObjectAttribute objectAttribute = new ObjectAttribute(this, name, property.type, property.required,
				property.multiple);
			localAttributes.put(objectAttribute.name, objectAttribute);
		}

		// Next, apply the attributes from the mandatory aspects as our own
		Map<String, Aspect> ma = new LinkedHashMap<>();
		for (Aspect aspect : this.mandatoryAspects) {
			ma.put(aspect.name, aspect);

			// Add the aspect's attributes
			for (String attributeName : aspect.getAllAttributeNames()) {
				ObjectAttribute attribute = aspect.getAttribute(attributeName);
				// If this attribute isn't declared on this type, or it's not declared in a
				// parent type, or it's not declared in another mandatory aspect, we add it...
				if (!localAttributes.containsKey(attributeName)) {
					localAttributes.put(attributeName, attribute);
				}
			}
		}

		this.mandatoryAspects = Tools.freezeMap(ma);
		this.localAttributes = Tools.freezeMap(new LinkedHashMap<>(localAttributes));

		ma = new LinkedHashMap<>();
		// First, get the inherited aspects from the mandatory aspects
		for (Aspect a : this.mandatoryAspects.values()) {
			while (a != null) {
				ma.put(a.name, a);
				a = a.getParent();
			}
		}

		// Now get the parent type's aspects
		Map<String, SchemaMember<?>> ancestors = new TreeMap<>();
		SchemaMember<T> p = this.parent;
		while (p != null) {
			ma.putAll(p.allAspects);
			ancestors.put(p.name, p);
			p = p.getParent();
		}

		this.allAspects = Tools.freezeMap(ma);
		ancestors.putAll(this.allAspects);
		this.ancestors = Tools.freezeMap(ancestors);

		// Finally, create the list of all the attributes this object supports
		Set<String> allAttributeNames = new TreeSet<>();
		allAttributeNames.addAll(this.localAttributes.keySet());
		if (parent != null) {
			allAttributeNames.addAll(parent.getAllAttributeNames());
		}
		this.allAttributeNames = Tools.freezeSet(new LinkedHashSet<>(allAttributeNames));

		Set<String> s = new TreeSet<>();
		s.addAll(this.ancestors.keySet());
		s.add(this.name);
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

	public Set<String> getAncestors() {
		return this.ancestors.keySet();
	}

	public Set<String> getAllAspects() {
		return this.allAspects.keySet();
	}

	public String getSignature() {
		return this.signature;
	}

	public String getName() {
		return this.name;
	}

	public T getParent() {
		return this.parent;
	}

	public Set<String> getMandatoryAspects() {
		return this.mandatoryAspects.keySet();
	}

	public boolean isDescendedOf(String name) {
		return this.ancestors.containsKey(name);
	}

	public ObjectAttribute getAttribute(String name) {
		if (!this.allAttributeNames.contains(name)) { return null; }
		ObjectAttribute objectAttribute = this.localAttributes.get(name);
		if (objectAttribute == null) {
			objectAttribute = this.parent.getAttribute(name);
		}
		return objectAttribute;
	}

	public Set<String> getAttributeNames() {
		return this.localAttributes.keySet();
	}

	public int getAttributeCount() {
		return this.localAttributes.size();
	}

	public Set<String> getAllAttributeNames() {
		return this.allAttributeNames;
	}

	public int getAllAttributeCount() {
		return this.allAttributeNames.size();
	}

	public boolean isAttributeInherited(String name) {
		if (this.parent == null) { return false; }
		return this.parent.getAllAttributeNames().contains(name);
	}

	public boolean isAspectInherited(String name) {
		if (this.parent == null) { return false; }
		return this.parent.getAllAspects().contains(name);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.parent);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		@SuppressWarnings("unchecked")
		SchemaMember<T> other = (SchemaMember<T>) obj;
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (!Tools.equals(this.parent, other.parent)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		final String type = getClass().getSimpleName();
		if (this.parent != null) { return String.format("%s [%s extends %s]", type, this.name, this.parent.name); }
		return String.format("%s [%s]", type, this.name);
	}
}