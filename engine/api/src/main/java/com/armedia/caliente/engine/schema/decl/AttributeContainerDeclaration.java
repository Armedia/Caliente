package com.armedia.caliente.engine.schema.decl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public class AttributeContainerDeclaration<T extends AttributeContainerDeclaration<T>> {

	private final String name;
	private final Map<String, AttributeDeclaration> attributes;
	private final Set<String> secondaries;
	private final T parent;

	public static String stripNamespace(String attName) {
		if (attName == null) { return null; }
		return attName.replaceAll("^\\w+:", "");
	}

	protected AttributeContainerDeclaration(String name, Collection<AttributeDeclaration> attributes) {
		this(name, attributes, null, null);
	}

	protected AttributeContainerDeclaration(String name, Collection<AttributeDeclaration> attributes,
		Collection<String> secondaries) {
		this(name, attributes, secondaries, null);
	}

	protected AttributeContainerDeclaration(String name, Collection<AttributeDeclaration> attributes, T parent) {
		this(name, attributes, null, parent);
	}

	protected AttributeContainerDeclaration(String name, Collection<AttributeDeclaration> attributes,
		Collection<String> secondaries, T parent) {
		this.name = name;
		if (attributes != null) {
			Map<String, AttributeDeclaration> m = new TreeMap<>();
			for (AttributeDeclaration attribute : attributes) {
				m.put(attribute.name, attribute);
			}
			this.attributes = Tools.freezeMap(new LinkedHashMap<>(m));
		} else {
			this.attributes = Collections.emptyMap();
		}
		if (secondaries != null) {
			this.secondaries = Tools.freezeSet(new LinkedHashSet<>(new TreeSet<>(secondaries)));
		} else {
			this.secondaries = Collections.emptySet();
		}
		this.parent = parent;
	}

	public final String getName() {
		return this.name;
	}

	public final Set<String> getSecondaries() {
		return this.secondaries;
	}

	public final T getParent() {
		return this.parent;
	}

	public final AttributeDeclaration getAttribute(String name) {
		return this.attributes.get(name);
	}

	public final boolean hasAttribute(String name) {
		return this.attributes.containsKey(name);
	}

	public final Set<String> getAttributeNames() {
		return this.attributes.keySet();
	}

	public final int getAttributeCount() {
		return this.attributes.size();
	}

	@Override
	public String toString() {
		return String.format("%s [name=%s, secondaries=%s, parent=%s]", getClass().getSimpleName(), this.name,
			this.secondaries, this.parent);
	}
}