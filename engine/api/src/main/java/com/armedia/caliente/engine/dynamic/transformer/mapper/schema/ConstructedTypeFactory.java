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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedLazySupplier;

public class ConstructedTypeFactory {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public static final boolean DEFAULT_EAGER = false;

	private final boolean eager;
	private final Map<String, CheckedLazySupplier<TypeDeclaration, SchemaServiceException>> objectTypeDeclarations;
	private final Map<String, CheckedLazySupplier<TypeDeclaration, SchemaServiceException>> secondaryTypeDeclarations;
	private final ConcurrentMap<String, CheckedLazySupplier<ConstructedType, SchemaServiceException>> constructedTypes = new ConcurrentHashMap<>();

	public ConstructedTypeFactory(SchemaService schemaService) throws SchemaServiceException {
		this(schemaService, null, null, false);
	}

	public ConstructedTypeFactory(SchemaService schemaService, boolean eager) throws SchemaServiceException {
		this(schemaService, null, null, ConstructedTypeFactory.DEFAULT_EAGER);
	}

	public ConstructedTypeFactory(SchemaService schemaService, Collection<String> usedTypes,
		Collection<String> usedSecondaries) throws SchemaServiceException {
		this(schemaService, usedTypes, usedSecondaries, ConstructedTypeFactory.DEFAULT_EAGER);
	}

	public ConstructedTypeFactory(SchemaService schemaService, Collection<String> usedTypes,
		Collection<String> usedSecondaries, boolean eager) throws SchemaServiceException {
		this.eager = eager;
		if (usedTypes == null) {
			usedTypes = schemaService.getObjectTypeNames();
		}
		Map<String, CheckedLazySupplier<TypeDeclaration, SchemaServiceException>> objectTypeDeclarations = new TreeMap<>();
		for (String typeName : usedTypes) {
			objectTypeDeclarations.put(typeName, new CheckedLazySupplier<>());
		}
		this.objectTypeDeclarations = Tools.freezeMap(objectTypeDeclarations);

		if (usedSecondaries == null) {
			usedSecondaries = schemaService.getSecondaryTypeNames();
		}
		Map<String, CheckedLazySupplier<TypeDeclaration, SchemaServiceException>> secondaryTypeDeclarations = new TreeMap<>();
		for (String secondaryTypeName : usedSecondaries) {
			secondaryTypeDeclarations.put(secondaryTypeName, new CheckedLazySupplier<>());
		}
		this.secondaryTypeDeclarations = Tools.freezeMap(secondaryTypeDeclarations);

		// Allow for eager loading of types/secondaries
		if (this.eager) {
			for (String objectTypeName : this.objectTypeDeclarations.keySet()) {
				getObjectTypeDeclaration(schemaService, objectTypeName);
			}
			for (String secondaryTypeName : this.secondaryTypeDeclarations.keySet()) {
				getObjectTypeDeclaration(schemaService, secondaryTypeName);
			}
		}
	}

	private void harvestData(final SchemaService schemaService, final TypeDeclaration base, final boolean secondary,
		final Map<String, AttributeDeclaration> attributes, final Set<String> hierarchy,
		final Set<String> secondariesVisited) throws SchemaServiceException {
		// Short-circuit - avoid recursion if already visited
		if (base == null) { return; }

		if (secondary) {
			// Ensure we add the current secondary to the visited secondaries set
			secondariesVisited.add(base.getName());
		}

		if (attributes != null) {
			for (AttributeDeclaration att : base.getAttributes()) {
				if (!attributes.containsKey(att.name)) {
					attributes.put(att.name, att);
				}
			}
		}

		for (String s : base.getSecondaries()) {
			// Only recurse if this is a new avenue not yet explored.
			if (!secondariesVisited.add(s)) {
				continue;
			}
			harvestData(schemaService, getSecondaryTypeDeclaration(schemaService, s), true, attributes, null,
				secondariesVisited);
		}

		final TypeDeclaration parent;
		if (secondary) {
			parent = getSecondaryTypeDeclaration(schemaService, base.getParentName());
		} else {
			parent = getObjectTypeDeclaration(schemaService, base.getParentName());
		}

		if ((parent != null) && ((hierarchy == null) || hierarchy.add(parent.getName()))) {
			harvestData(schemaService, parent, secondary, attributes, hierarchy, secondariesVisited);
		}
	}

	protected String getSignature(TypeDeclaration type, Set<String> secondaries) {
		String s = String.format("{%s}:%s", type.getName(), new TreeSet<>(secondaries));
		return DigestUtils.sha256Hex(s);
	}

	protected final TypeDeclaration getObjectTypeDeclaration(final SchemaService schemaService, final String typeName)
		throws SchemaServiceException {
		if (StringUtils.isBlank(typeName)) { return null; }
		CheckedLazySupplier<TypeDeclaration, SchemaServiceException> ret = this.objectTypeDeclarations.get(typeName);
		if (ret == null) { return null; }
		try {
			return ret.getChecked(() -> Objects
				.requireNonNull(schemaService, "Must provide a non-null SchemaService instance for lazy initialization")
				.getObjectTypeDeclaration(typeName));
		} catch (Exception e) {
			if (SchemaServiceException.class.isInstance(e)) { throw SchemaServiceException.class.cast(e); }
			throw new SchemaServiceException(String.format(
				"Unexpected initializer exception trying to retrieve the type declaration for [%s]", typeName), e);
		}
	}

	protected final TypeDeclaration getSecondaryTypeDeclaration(final SchemaService schemaService,
		final String secondaryTypeName) throws SchemaServiceException {
		if (StringUtils.isBlank(secondaryTypeName)) { return null; }
		CheckedLazySupplier<TypeDeclaration, SchemaServiceException> ret = this.secondaryTypeDeclarations
			.get(secondaryTypeName);
		if (ret == null) { return null; }

		try {
			return ret.getChecked(() -> Objects
				.requireNonNull(schemaService, "Must provide a non-null SchemaService instance for lazy initialization")
				.getSecondaryTypeDeclaration(secondaryTypeName));
		} catch (Exception e) {
			if (SchemaServiceException.class.isInstance(e)) { throw SchemaServiceException.class.cast(e); }
			throw new SchemaServiceException(String.format(
				"Unexpected initializer exception trying to retrieve the secondary type declaration for [%s]",
				secondaryTypeName), e);
		}
	}

	public final ConstructedType constructType(final SchemaService schemaService, final String typeName,
		Collection<String> secondaries) throws SchemaServiceException {
		if (StringUtils.isBlank(typeName)) {
			throw new IllegalArgumentException("Must provide a non-null, non-empty type name");
		}

		final TypeDeclaration mainType = getObjectTypeDeclaration(schemaService, typeName);
		if (mainType == null) { return null; }

		if ((secondaries == null) || secondaries.isEmpty()) {
			secondaries = Collections.emptySet();
		}

		final Set<String> allSecondaries = new HashSet<>();

		// First, go through the explicitly added secondaries
		for (String s : secondaries) {
			TypeDeclaration declaration = getSecondaryTypeDeclaration(schemaService, s);
			if (declaration != null) {
				harvestData(schemaService, declaration, true, null, null, allSecondaries);
			}
		}

		// We specifically don't harvest attributes in this pass because we're just looking
		// for the complete list of secondaries that decorate this type
		harvestData(schemaService, mainType, false, null, null, allSecondaries);
		final String signature = getSignature(mainType, allSecondaries);
		CheckedLazySupplier<ConstructedType, SchemaServiceException> ret = ConcurrentUtils.createIfAbsentUnchecked(
			this.constructedTypes, signature,
			() -> new CheckedLazySupplier<>(() -> newObjectType(
				Objects.requireNonNull(schemaService,
					"Must provide a non-null SchemaService instance for lazy initialization"),
				mainType, allSecondaries, signature)));

		try {
			return ret.getChecked();
		} catch (Exception e) {
			// Recurse...just in case...
			if (SchemaServiceException.class.isInstance(e)) { throw SchemaServiceException.class.cast(e); }
			throw new SchemaServiceException(
				String.format("Unexpected initializer exception trying to construct the type [%s] with secondaries %s",
					typeName, allSecondaries),
				e);
		}
	}

	protected ConstructedType newObjectType(final SchemaService schemaService, TypeDeclaration mainType,
		Collection<String> secondaries, String signature) throws SchemaServiceException {

		final Map<String, AttributeDeclaration> attributes = new TreeMap<>();
		final Set<String> ancestors = new LinkedHashSet<>();
		final Set<String> visited = new HashSet<>();

		for (String s : secondaries) {
			TypeDeclaration S = getSecondaryTypeDeclaration(schemaService, s);
			if (S != null) {
				harvestData(schemaService, S, true, attributes, null, visited);
			}
		}

		harvestData(schemaService, mainType, false, attributes, ancestors, visited);

		// Make sure...just in case ;)
		ancestors.remove(mainType.getName());

		// At this point we have all the secondaries in *visited*, and we have
		// harvested all the attributes associated to this type...
		return new ConstructedType(mainType, ancestors, visited, attributes, signature);
	}

	public boolean hasType(String name) {
		return this.objectTypeDeclarations.containsKey(name);
	}

	public boolean hasSecondaryType(String name) {
		return this.secondaryTypeDeclarations.containsKey(name);
	}
}