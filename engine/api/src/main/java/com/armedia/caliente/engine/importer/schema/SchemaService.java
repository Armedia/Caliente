package com.armedia.caliente.engine.importer.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.importer.schema.decl.SchemaDeclarationService;
import com.armedia.caliente.engine.importer.schema.decl.SchemaDeclarationServiceException;
import com.armedia.caliente.engine.importer.schema.decl.SecondaryTypeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.TypeDeclaration;
import com.armedia.commons.utilities.LazyInitializer;
import com.armedia.commons.utilities.Tools;

public class SchemaService {

	private final SchemaDeclarationService declarationService;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<String, LazyInitializer<ObjectType>> constructedTypes = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, LazyInitializer<TypeDeclaration>> typeDeclarations = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, LazyInitializer<SecondaryTypeDeclaration>> secondaryTypeDeclarations = new ConcurrentHashMap<>();

	public SchemaService(SchemaDeclarationService declarationService) {
		this.declarationService = declarationService;
	}

	protected String getSignature(TypeDeclaration type, Map<String, SecondaryTypeDeclaration> secondaries) {
		String s = String.format("{%s}:%s", type.getName(), new TreeSet<>(secondaries.keySet()));
		return DigestUtils.sha256Hex(s);
	}

	protected final TypeDeclaration getTypeDeclaration(final String typeName) throws SchemaDeclarationServiceException {
		if (StringUtils.isBlank(typeName)) { return null; }
		LazyInitializer<TypeDeclaration> ret = ConcurrentUtils.createIfAbsentUnchecked(this.typeDeclarations, typeName,
			new ConcurrentInitializer<LazyInitializer<TypeDeclaration>>() {
				@Override
				public LazyInitializer<TypeDeclaration> get() {
					return new LazyInitializer<>(() -> {
						try {
							TypeDeclaration declaration = SchemaService.this.declarationService
								.getTypeDeclaration(typeName);
							if (declaration == null) {
								declaration = TypeDeclaration.NULL;
							}
							return declaration;
						} catch (SchemaDeclarationServiceException e) {
							throw new SchemaServiceException(
								String.format("Failed to get the declaration for type [%s]", typeName), e);
						}
					});
				}
			});

		TypeDeclaration declaration = null;
		try {
			declaration = ret.get();
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
		return (declaration != TypeDeclaration.NULL ? declaration : null);
	}

	protected final SecondaryTypeDeclaration getSecondaryTypeDeclaration(final String secondaryTypeName)
		throws SchemaDeclarationServiceException {
		if (StringUtils.isBlank(secondaryTypeName)) { return null; }
		LazyInitializer<SecondaryTypeDeclaration> ret = ConcurrentUtils.createIfAbsentUnchecked(
			this.secondaryTypeDeclarations, secondaryTypeName,
			new ConcurrentInitializer<LazyInitializer<SecondaryTypeDeclaration>>() {
				@Override
				public LazyInitializer<SecondaryTypeDeclaration> get() {
					return new LazyInitializer<>(() -> {
						try {
							SecondaryTypeDeclaration declaration = SchemaService.this.declarationService
								.getSecondaryTypeDeclaration(secondaryTypeName);
							if (declaration == null) {
								declaration = SecondaryTypeDeclaration.NULL;
							}
							return declaration;
						} catch (SchemaDeclarationServiceException e) {
							throw new SchemaServiceException(
								String.format("Failed to get the declaration for type [%s]", secondaryTypeName), e);
						}
					});
				}
			});

		SecondaryTypeDeclaration declaration = null;
		try {
			declaration = ret.get();
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
		return (declaration != SecondaryTypeDeclaration.NULL ? declaration : null);
	}

	public final ObjectType constructType(final String typeName, final Collection<String> secondaryTypeNames)
		throws SchemaDeclarationServiceException {
		if (StringUtils
			.isBlank(typeName)) { throw new IllegalArgumentException("Must provide a non-null, non-empty type name"); }

		final TypeDeclaration mainDeclaration = getTypeDeclaration(typeName);
		if (mainDeclaration == null) { return null; }

		final Collection<String> secondaries;
		if ((secondaryTypeNames == null) || secondaryTypeNames.isEmpty()) {
			secondaries = Collections.emptyList();
		} else {
			secondaries = Tools.freezeCollection(new ArrayList<>(secondaryTypeNames));
		}

		final Queue<Pair<String, String>> typeQueue = new LinkedList<>();
		final Queue<Triple<String, Boolean, String>> secondaryTypeQueue = new LinkedList<>();

		for (String s : mainDeclaration.getSecondaries()) {
			typeQueue.add(Pair.of(mainDeclaration.getName(), s));
		}

		for (String s : secondaries) {
			secondaryTypeQueue.add(Triple.of(null, false, s));
		}

		final Map<String, TypeDeclaration> typeHierarchy = new LinkedHashMap<>();
		typeHierarchy.put(mainDeclaration.getName(), mainDeclaration);

		typeQueue.add(Pair.of(mainDeclaration.getName(), mainDeclaration.getParentName()));
		while (!typeQueue.isEmpty()) {
			final Pair<String, String> next = typeQueue.poll();
			final String prevTypeName = next.getLeft();
			final String nextTypeName = next.getRight();
			if (StringUtils.isBlank(nextTypeName)) {
				continue;
			}

			if (typeHierarchy.containsKey(nextTypeName)) {
				// Avoid duplicate searches, though they shouldn't be too expensive...
				continue;
			}

			TypeDeclaration nextTypeDeclaration = getTypeDeclaration(nextTypeName);
			if (nextTypeDeclaration == null) {
				this.log.debug(
					"The type [{}] references the missing type [{}] as its parent, this may lead to errant behavior during transformation and mapping",
					prevTypeName, nextTypeName);
				continue;
			}

			typeHierarchy.put(nextTypeDeclaration.getName(), nextTypeDeclaration);
			if (!StringUtils.isBlank(nextTypeDeclaration.getParentName())) {
				typeQueue.add(Pair.of(nextTypeDeclaration.getName(), nextTypeDeclaration.getParentName()));
			}
			for (String s : nextTypeDeclaration.getSecondaries()) {
				if (!StringUtils.isBlank(s)) {
					secondaryTypeQueue.add(Triple.of(nextTypeDeclaration.getName(), true, s));
				}
			}
		}

		final Map<String, SecondaryTypeDeclaration> secondaryDeclarations = new TreeMap<>();
		while (!secondaryTypeQueue.isEmpty()) {
			final Triple<String, Boolean, String> next = secondaryTypeQueue.poll();
			final String refName = next.getLeft();
			final boolean isType = next.getMiddle();
			final String nextSecondaryName = next.getRight();

			if (StringUtils.isBlank(nextSecondaryName)) {
				continue;
			}

			if (secondaryDeclarations.containsKey(nextSecondaryName)) {
				// Avoid duplicate searches, though they shouldn't be too expensive...
				continue;
			}

			final SecondaryTypeDeclaration declaration = getSecondaryTypeDeclaration(nextSecondaryName);
			if (declaration == null) {
				this.log.debug(
					"The {}type [{}] references the missing secondary type [{}], this may lead to errant behavior during transformation and mapping",
					isType ? "" : "secondary ", refName, nextSecondaryName);
				continue;
			}

			secondaryDeclarations.put(declaration.getName(), declaration);
			if (!StringUtils.isBlank(declaration.getParentName())) {
				secondaryTypeQueue.add(Triple.of(declaration.getName(), false, declaration.getParentName()));
			}
			for (String s : declaration.getSecondaries()) {
				if (!StringUtils.isBlank(s)) {
					secondaryTypeQueue.add(Triple.of(declaration.getName(), false, s));
				}
			}
		}

		// At this point we can start traversing the tree upwards to build out the hierarchy
		final String signature = getSignature(mainDeclaration, secondaryDeclarations);
		LazyInitializer<ObjectType> ret = ConcurrentUtils.createIfAbsentUnchecked(this.constructedTypes, signature,
			new ConcurrentInitializer<LazyInitializer<ObjectType>>() {
				@Override
				public LazyInitializer<ObjectType> get() {
					return new LazyInitializer<>(() -> {
						try {
							ObjectType constructedType = constructType(mainDeclaration, typeHierarchy,
								secondaryDeclarations);
							if (constructedType == null) {
								constructedType = ObjectType.NULL;
							}
							return constructedType;
						} catch (SchemaDeclarationServiceException e) {
							throw new SchemaServiceException(String.format(
								"Failed to get construct the type [%s] with secondaries %s", typeName, secondaries), e);
						}
					});
				}
			});

		ObjectType constructedType = null;
		try {
			constructedType = ret.get();
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
					typeName, secondaries),
				e);
		}
		return (constructedType != ObjectType.NULL ? constructedType : null);
	}

	public final SecondaryType constructSecondaryType(String name) throws SchemaDeclarationServiceException {
		SecondaryTypeDeclaration declaration = getSecondaryTypeDeclaration(name);
		if (declaration == null) { return null; }
		return constructSecondaryType(declaration);
	}

	protected SecondaryType constructSecondaryType(SecondaryTypeDeclaration declaration)
		throws SchemaDeclarationServiceException {
		if (declaration == null) { return null; }
		final SecondaryType parent = constructSecondaryType(declaration.getParentName());
		final Queue<String> queue = new LinkedList<>();
		if (parent != null) {
			queue.add(parent.getName());
		}
		queue.addAll(declaration.getSecondaries());
		while (!queue.isEmpty()) {

		}

		return null;
	}

	protected ObjectType constructType(TypeDeclaration type, Map<String, TypeDeclaration> typeHierarchy,
		Map<String, SecondaryTypeDeclaration> secondaries) throws SchemaDeclarationServiceException {

		return null;
	}

	public boolean hasType(String name) {
		return false;
	}

	public boolean hasSecondaryType(String name) {
		return false;
	}
}