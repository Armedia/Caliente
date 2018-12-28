package com.armedia.caliente.engine.importer.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.caliente.engine.importer.schema.decl.AttributeContainerDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.AttributeDeclaration;
import com.armedia.commons.utilities.Tools;

public abstract class SchemaMember<T extends SchemaMember<T, D>, D extends AttributeContainerDeclaration> {

	private final D declaration;
	private final String name;
	private final Set<String> ancestors;
	private final Set<String> secondaries;
	private final Map<String, AttributeDeclaration> attributes;

	private final String signature;

	protected SchemaMember() {
		this.declaration = null;
		this.name = null;
		this.ancestors = Collections.emptySet();
		this.secondaries = Collections.emptySet();
		this.attributes = Collections.emptyMap();
		this.signature = null;
	}

	SchemaMember(D declaration, Set<String> ancestors, Set<String> secondaries,
		Map<String, AttributeDeclaration> attributes, String signature) {
		this.declaration = Objects.requireNonNull(declaration, "Must provide a non-null declaration");
		this.name = declaration.getName();
		this.ancestors = Tools.freezeSet(new LinkedHashSet<>(ancestors));
		this.attributes = Tools.freezeMap(new LinkedHashMap<>(new TreeMap<>(attributes)));
		this.secondaries = Tools.freezeSet(new LinkedHashSet<>(new TreeSet<>(secondaries)));
		this.signature = signature;
	}

	public D getDeclaration() {
		return this.declaration;
	}

	public String getName() {
		return this.name;
	}

	public Set<String> getAncestors() {
		return this.ancestors;
	}

	public String getSignature() {
		return this.signature;
	}

	public Set<String> getSecondaries() {
		return this.secondaries;
	}

	public boolean hasAttribute(String name) {
		return this.attributes.containsKey(name);
	}

	public AttributeDeclaration getAttribute(String name) {
		return this.attributes.get(name);
	}

	public Set<String> getAttributeNames() {
		return this.attributes.keySet();
	}

	public int getAttributeCount() {
		return this.attributes.size();
	}
}