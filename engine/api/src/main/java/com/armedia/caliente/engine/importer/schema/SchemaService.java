package com.armedia.caliente.engine.importer.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.importer.schema.decl.AttributeContainerDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.AttributeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.ObjectTypeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.SchemaDeclarationService;
import com.armedia.caliente.engine.importer.schema.decl.SchemaDeclarationServiceException;
import com.armedia.caliente.engine.importer.schema.decl.SecondaryTypeDeclaration;
import com.armedia.commons.utilities.LazyInitializer;
import com.armedia.commons.utilities.Tools;

public class SchemaService {

	private final SchemaDeclarationService declarationService;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<String, LazyInitializer<ObjectTypeDeclaration>> objectTypeDeclarations;
	private final Map<String, LazyInitializer<SecondaryTypeDeclaration>> secondaryTypeDeclarations;
	private final ConcurrentMap<String, LazyInitializer<ConstructedType>> constructedTypes = new ConcurrentHashMap<>();

	public SchemaService(SchemaDeclarationService declarationService) throws SchemaDeclarationServiceException {
		this.declarationService = declarationService;

		Map<String, LazyInitializer<ObjectTypeDeclaration>> objectTypeDeclarations = new TreeMap<>();
		for (String typeName : declarationService.getObjectTypeNames()) {
			objectTypeDeclarations.put(typeName, new LazyInitializer<>(() -> {
				try {
					return SchemaService.this.declarationService.getObjectTypeDeclaration(typeName);
				} catch (SchemaDeclarationServiceException e) {
					throw new SchemaServiceException(
						String.format("Failed to get the declaration for type [%s]", typeName), e);
				}
			}));
		}
		this.objectTypeDeclarations = Tools.freezeMap(objectTypeDeclarations);

		Map<String, LazyInitializer<SecondaryTypeDeclaration>> secondaryTypeDeclarations = new TreeMap<>();
		for (String typeName : declarationService.getSecondaryTypeNames()) {
			secondaryTypeDeclarations.put(typeName, new LazyInitializer<>(() -> {
				try {
					return SchemaService.this.declarationService.getSecondaryTypeDeclaration(typeName);
				} catch (SchemaDeclarationServiceException e) {
					throw new SchemaServiceException(
						String.format("Failed to get the declaration for type [%s]", typeName), e);
				}
			}));
		}
		this.secondaryTypeDeclarations = Tools.freezeMap(secondaryTypeDeclarations);
	}

	private void harvestData(AttributeContainerDeclaration base, Map<String, AttributeDeclaration> attributes,
		final Set<String> hierarchy, final Set<String> secondariesVisited) throws SchemaDeclarationServiceException {
		// Short-circuit - avoid recursion if already visited
		if (base == null) { return; }

		if (SecondaryTypeDeclaration.class.isInstance(base)) {
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
			harvestData(getSecondaryTypeDeclaration(s), attributes, null, secondariesVisited);
		}

		final AttributeContainerDeclaration parent;
		if (SecondaryTypeDeclaration.class.isInstance(base)) {
			parent = getSecondaryTypeDeclaration(base.getParentName());
		} else if (ObjectTypeDeclaration.class.isInstance(base)) {
			parent = getObjectTypeDeclaration(base.getParentName());
		} else {
			parent = null;
		}

		if ((parent != null) && ((hierarchy == null) || hierarchy.add(parent.getName()))) {
			harvestData(parent, attributes, hierarchy, secondariesVisited);
		}
	}

	protected String getSignature(ObjectTypeDeclaration type, Set<String> secondaries) {
		String s = String.format("{%s}:%s", type.getName(), new TreeSet<>(secondaries));
		return DigestUtils.sha256Hex(s);
	}

	protected final ObjectTypeDeclaration getObjectTypeDeclaration(final String typeName)
		throws SchemaDeclarationServiceException {
		if (StringUtils.isBlank(typeName)) { return null; }
		LazyInitializer<ObjectTypeDeclaration> ret = this.objectTypeDeclarations.get(typeName);
		if (ret == null) { return null; }
		try {
			return ret.get();
		} catch (ConcurrentException e) {
			Throwable t = e.getCause();
			// Recurse...just in case...
			while (ConcurrentException.class.isInstance(t)) {
				t = t.getCause();
			}
			while (SchemaServiceException.class.isInstance(t)) {
				t = t.getCause();
			}
			if (SchemaDeclarationServiceException.class
				.isInstance(t)) { throw SchemaDeclarationServiceException.class.cast(t); }
			throw new SchemaDeclarationServiceException(String.format(
				"Unexpected initializer exception trying to retrieve the type declaration for [%s]", typeName), e);
		}
	}

	protected final SecondaryTypeDeclaration getSecondaryTypeDeclaration(final String secondaryTypeName)
		throws SchemaDeclarationServiceException {
		if (StringUtils.isBlank(secondaryTypeName)) { return null; }
		LazyInitializer<SecondaryTypeDeclaration> ret = this.secondaryTypeDeclarations.get(secondaryTypeName);
		if (ret == null) { return null; }

		try {
			return ret.get();
		} catch (ConcurrentException e) {
			Throwable t = e.getCause();
			// Recurse...just in case...
			while (ConcurrentException.class.isInstance(t)) {
				t = t.getCause();
			}
			while (SchemaServiceException.class.isInstance(t)) {
				t = t.getCause();
			}
			if (SchemaDeclarationServiceException.class
				.isInstance(t)) { throw SchemaDeclarationServiceException.class.cast(t); }
			throw new SchemaDeclarationServiceException(String.format(
				"Unexpected initializer exception trying to retrieve the secondary type declaration for [%s]",
				secondaryTypeName), e);
		}
	}

	public final ConstructedType constructType(final String typeName, Collection<String> secondaries)
		throws SchemaDeclarationServiceException {
		if (StringUtils
			.isBlank(typeName)) { throw new IllegalArgumentException("Must provide a non-null, non-empty type name"); }

		final ObjectTypeDeclaration mainType = getObjectTypeDeclaration(typeName);
		if (mainType == null) { return null; }

		if ((secondaries == null) || secondaries.isEmpty()) {
			secondaries = Collections.emptySet();
		}

		final Set<String> allSecondaries = new HashSet<>();

		// First, go through the explicitly added secondaries
		for (String s : secondaries) {
			SecondaryTypeDeclaration S = getSecondaryTypeDeclaration(s);
			if (S != null) {
				harvestData(S, null, null, allSecondaries);
			}
		}

		// We specifically don't harvest attributes in this pass because we're just looking
		// for the complete list of secondaries that decorate this type
		harvestData(mainType, null, null, allSecondaries);
		final String signature = getSignature(mainType, allSecondaries);

		LazyInitializer<ConstructedType> ret = ConcurrentUtils.createIfAbsentUnchecked(this.constructedTypes, signature,
			new ConcurrentInitializer<LazyInitializer<ConstructedType>>() {
				@Override
				public LazyInitializer<ConstructedType> get() {
					return new LazyInitializer<>(() -> {
						try {
							return newObjectType(mainType, allSecondaries, signature);
						} catch (SchemaDeclarationServiceException e) {
							throw new SchemaServiceException(
								String.format("Failed to get construct the type [%s] with secondaries %s", typeName,
									allSecondaries),
								e);
						}
					});
				}
			});

		try {
			return ret.get();
		} catch (ConcurrentException e) {
			Throwable t = e.getCause();
			// Recurse...just in case...
			while (ConcurrentException.class.isInstance(t)) {
				t = t.getCause();
			}
			while (SchemaServiceException.class.isInstance(t)) {
				t = t.getCause();
			}
			if (SchemaDeclarationServiceException.class
				.isInstance(t)) { throw SchemaDeclarationServiceException.class.cast(t); }
			throw new SchemaDeclarationServiceException(
				String.format("Unexpected initializer exception trying to construct the type [%s] with secondaries %s",
					typeName, allSecondaries),
				e);
		}
	}

	protected ConstructedType newObjectType(ObjectTypeDeclaration mainType, Collection<String> secondaries,
		String signature) throws SchemaDeclarationServiceException {

		final Map<String, AttributeDeclaration> attributes = new TreeMap<>();
		final Set<String> ancestors = new LinkedHashSet<>();
		final Set<String> visited = new HashSet<>();

		for (String s : secondaries) {
			SecondaryTypeDeclaration S = getSecondaryTypeDeclaration(s);
			if (S != null) {
				harvestData(S, attributes, null, visited);
			}
		}

		harvestData(mainType, attributes, ancestors, visited);

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