package com.armedia.caliente.engine.importer.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;

import com.armedia.caliente.engine.importer.schema.SchemaContentModel.Aspect;
import com.armedia.caliente.engine.importer.schema.decl.AttributeContainerDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.AttributeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.SecondaryTypeDeclaration;
import com.armedia.commons.utilities.Tools;

public abstract class SchemaMember<T extends SchemaMember<T, D>, D extends AttributeContainerDeclaration<D>> {

	protected final String name;
	protected final D declaration;
	protected final String parentName;
	protected final T parent;

	protected final Map<String, T> ancestors;
	protected final Map<String, SecondaryType> secondaries;
	protected final Map<String, SecondaryType> allSecondaries;
	protected final Map<String, AttributeDeclaration<D>> attributes;
	protected final Map<String, AttributeDeclaration<D>> allAttributes;
	protected final Set<String> allAttributeNames;
	protected final String signature;

	protected SchemaMember(String nothing) {
		this.declaration = null;
		this.name = null;
		this.parentName = null;
		this.parent = null;
		this.ancestors = Collections.emptyMap();
		this.secondaries = this.allSecondaries = Collections.emptyMap();
		this.attributes = this.allAttributes = Collections.emptyMap();
		this.allAttributeNames = Collections.emptySet();
		this.signature = null;
	}

	SchemaMember(D declaration, Map<String, D> hierarchy, Map<String, SecondaryTypeDeclaration> secondaries) {
		this.declaration = Objects.requireNonNull(declaration, "Must provide a non-null declaration");
		this.name = declaration.getName();
		this.parentName = declaration.getParentName();

		Map<String, AttributeDeclaration<D>> localAttributes = new TreeMap<>();
		for (AttributeDeclaration<D> attribute : declaration.getAttributes()) {
			String attributeName = attribute.name;
			// Is this a duplicate at this level?
			if (localAttributes.containsKey(attributeName)) { throw new IllegalStateException(
				String.format("Duplicate attribute name [%s] on type [%s]", attributeName, this.name)); }

			// Is this a duplicate at my parent's level?
			if ((this.parent != null)
				&& (this.parent.getAttribute(attributeName) != null)) { throw new IllegalStateException(
					String.format("Duplicate attribute name [%s] on type [%s] - already defined by supertype",
						attributeName, this.name)); }

			// No dupes, add the attribute
			localAttributes.put(attribute.name, attribute);
		}

		// Next, apply the attributes from the mandatory aspects as our own
		Map<String, SecondaryType> includedSecondaries = new LinkedHashMap<>();
		if (secondaries == null) {
			secondaries = Collections.emptyMap();
		}
		for (String secondaryName : secondaries.keySet()) {
			SecondaryTypeDeclaration secondaryDeclaration = secondaries.get(secondaryName);

			SecondaryType st = schemaService.getSecondaryType(secondaryName);
			includedSecondaries.put(st.getName(), st);

			// Add the aspect's attributes
			for (String attributeName : st.getAllAttributeNames()) {
				AttributeDeclaration<D> attribute = st.getAttribute(attributeName);
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
		if (this.parent != null) {
			allAttributeNames.addAll(this.parent.getAllAttributeNames());
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

	public AttributeDeclaration<D> getAttribute(String name) {
		if (!this.allAttributeNames.contains(name)) { return null; }
		AttributeDeclaration<D> objectAttribute = this.localAttributes.get(name);
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