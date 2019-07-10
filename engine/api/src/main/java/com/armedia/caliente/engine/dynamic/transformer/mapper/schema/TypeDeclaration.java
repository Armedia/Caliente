/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic.transformer.mapper.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public class TypeDeclaration {

	private final String name;
	private final Map<String, AttributeDeclaration> attributes;
	private final Set<String> secondaries;
	private final String parentName;

	public static String stripNamespace(String attName) {
		if (attName == null) { return null; }
		return attName.replaceAll("^\\w+:", "");
	}

	public TypeDeclaration(String name) {
		this(name, null, null, null);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration> attributes) {
		this(name, attributes, null, null);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration> attributes, Collection<String> secondaries) {
		this(name, attributes, secondaries, null);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration> attributes, String parentName) {
		this(name, attributes, null, parentName);
	}

	public TypeDeclaration(String name, Collection<AttributeDeclaration> attributes, Collection<String> secondaries,
		String parentName) {
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
		this.parentName = parentName;
	}

	public final String getName() {
		return this.name;
	}

	public final Set<String> getSecondaries() {
		return this.secondaries;
	}

	public final String getParentName() {
		return this.parentName;
	}

	public final Collection<AttributeDeclaration> getAttributes() {
		return this.attributes.values();
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
			this.secondaries, this.parentName);
	}
}