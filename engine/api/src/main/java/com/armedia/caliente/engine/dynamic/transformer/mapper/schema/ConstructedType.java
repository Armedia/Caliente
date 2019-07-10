/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public class ConstructedType {

	private final TypeDeclaration declaration;
	private final String name;
	private final Set<String> ancestors;
	private final Set<String> secondaries;
	private final Map<String, AttributeDeclaration> attributes;

	private final String signature;

	ConstructedType(TypeDeclaration declaration, Set<String> ancestors, Set<String> secondaries,
		Map<String, AttributeDeclaration> attributes, String signature) {
		this.declaration = Objects.requireNonNull(declaration, "Must provide a non-null declaration");
		this.name = declaration.getName();
		this.ancestors = Tools.freezeSet(new LinkedHashSet<>(ancestors));
		this.attributes = Tools.freezeMap(new LinkedHashMap<>(new TreeMap<>(attributes)));
		this.secondaries = Tools.freezeSet(new LinkedHashSet<>(new TreeSet<>(secondaries)));
		this.signature = signature;
	}

	public TypeDeclaration getDeclaration() {
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

	@Override
	public String toString() {
		return String.format("{%s}%s", this.name, this.secondaries);
	}
}