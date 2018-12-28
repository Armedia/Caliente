package com.armedia.caliente.engine.importer.schema;

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
import com.armedia.caliente.engine.importer.schema.decl.SchemaServiceException;
import com.armedia.caliente.engine.importer.schema.decl.SecondaryTypeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.TypeDeclaration;
import com.armedia.commons.utilities.LockDispenser;

public class SchemaService {

	private final SchemaDeclarationService declarationService;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private class SyncInit<T> {
		private T value = null;
	}

	private final LockDispenser<String, SyncInit<ObjectType>> constructedLocks = new LockDispenser<String, SchemaService.SyncInit<ObjectType>>() {
		@Override
		protected SyncInit<ObjectType> newLock(String key) {
			return new SyncInit<>();
		}
	};

	private final ConcurrentMap<String, ObjectType> constructedTypes = new ConcurrentHashMap<>();

	private final LockDispenser<String, SyncInit<TypeDeclaration>> typeLocks = LockDispenser.getBasic();
	private final ConcurrentMap<String, TypeDeclaration> typeDeclarations = new ConcurrentHashMap<>();

	private final LockDispenser<String, SyncInit<SecondaryTypeDeclaration>> secondaryLocks = LockDispenser.getBasic();
	private final ConcurrentMap<String, SecondaryTypeDeclaration> secondaryTypeDeclarations = new ConcurrentHashMap<>();

	public SchemaService(SchemaDeclarationService declarationService) {
		this.declarationService = declarationService;
	}

	protected String getSignature(TypeDeclaration type, Map<String, SecondaryTypeDeclaration> secondaries) {
		String s = String.format("%s|%s", type.getName(), new TreeSet<>(secondaries.keySet()));
		return DigestUtils.sha256Hex(s);
	}

	protected final TypeDeclaration getTypeDeclaration(String typeName) throws SchemaServiceException {
		final TypeDeclaration ret;
		if (StringUtils.isBlank(typeName)) { return null; }
		synchronized (this.typeLocks.getLock(typeName)) {
			try {
				ret = ConcurrentUtils.createIfAbsent(this.typeDeclarations, typeName,
					new ConcurrentInitializer<TypeDeclaration>() {
						@Override
						public TypeDeclaration get() throws ConcurrentException {
							try {
								TypeDeclaration declaration = SchemaService.this.declarationService
									.getTypeDeclaration(typeName);
								if (declaration == null) {
									declaration = TypeDeclaration.NULL;
								}
								return declaration;
							} catch (SchemaServiceException e) {
								throw new ConcurrentException(
									String.format("Failed to get the declaration for type [%s]", typeName), e);
							}
						}
					});
			} catch (ConcurrentException e) {
				Throwable t = e.getCause();
				if (SchemaServiceException.class.isInstance(t)) { throw SchemaServiceException.class.cast(t); }
				throw new SchemaServiceException(String.format(
					"Unexpected initializer exception trying to retrieve the type declaration for [%s]", typeName), e);
			}
		}
		return (ret != TypeDeclaration.NULL ? ret : null);
	}

	protected final SecondaryTypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName)
		throws SchemaServiceException {
		if (StringUtils.isBlank(secondaryTypeName)) { return null; }
		final SecondaryTypeDeclaration ret;
		synchronized (this.secondaryLocks.getLock(secondaryTypeName)) {
			try {
				ret = ConcurrentUtils.createIfAbsent(this.secondaryTypeDeclarations, secondaryTypeName,
					new ConcurrentInitializer<SecondaryTypeDeclaration>() {
						@Override
						public SecondaryTypeDeclaration get() throws ConcurrentException {
							try {
								SecondaryTypeDeclaration declaration = SchemaService.this.declarationService
									.getSecondaryTypeDeclaration(secondaryTypeName);
								if (declaration == null) {
									declaration = SecondaryTypeDeclaration.NULL;
								}
								return declaration;
							} catch (SchemaServiceException e) {
								throw new ConcurrentException(String.format(
									"Failed to get the declaration for secondary type [%s]", secondaryTypeName), e);
							}
						}
					});
			} catch (ConcurrentException e) {
				Throwable t = e.getCause();
				if (SchemaServiceException.class.isInstance(t)) { throw SchemaServiceException.class.cast(t); }
				throw new SchemaServiceException(String.format(
					"Unexpected initializer exception trying to retrieve the secondary type declaration for [%s]",
					secondaryTypeName), e);
			}
		}
		return (ret != SecondaryTypeDeclaration.NULL ? ret : null);
	}

	public final ObjectType constructType(final String typeName, Collection<String> secondaries)
		throws SchemaServiceException {
		if (StringUtils
			.isBlank(typeName)) { throw new IllegalArgumentException("Must provide a non-null, non-empty type name"); }

		final TypeDeclaration mainDeclaration = getTypeDeclaration(typeName);
		if (mainDeclaration == null) { return null; }

		if ((secondaries == null) || secondaries.isEmpty()) {
			secondaries = Collections.emptyList();
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
		final Object constructedLock = this.constructedLocks.getLock(signature);
		final ObjectType constructedType;
		synchronized (constructedLock) {
			try {
				constructedType = ConcurrentUtils.createIfAbsent(this.constructedTypes, signature,
					new ConcurrentInitializer<ObjectType>() {
						@Override
						public ObjectType get() throws ConcurrentException {
							try {
								ObjectType constructedType = constructType(mainDeclaration, typeHierarchy,
									secondaryDeclarations);
								if (constructedType == null) {
									constructedType = ObjectType.NULL;
								}
								return constructedType;
							} catch (SchemaServiceException e) {
								throw new ConcurrentException(
									String.format("Failed to construct the object type for [%s] with secondaries %s",
										mainDeclaration.getName(), secondaryDeclarations.keySet()),
									e);
							}
						}
					});
			} catch (ConcurrentException e) {
				Throwable t = e.getCause();
				if (SchemaServiceException.class.isInstance(t)) { throw SchemaServiceException.class.cast(t); }
				throw new SchemaServiceException(String.format(
					"Unexpected initializer exception constructing the object type for [%s] with secondaries %s",
					mainDeclaration.getName(), secondaryDeclarations.keySet()), e);
			}
		}

		// No main type? Can't continue...
		if (ObjectType.NULL == constructedType) { return null; }
		return constructedType;
	}

	public final SecondaryType constructSecondaryType(String name) throws SchemaServiceException {
		SecondaryTypeDeclaration declaration = getSecondaryTypeDeclaration(name);
		if (declaration == null) { return null; }
		return constructSecondaryType(declaration);
	}

	protected SecondaryType constructSecondaryType(SecondaryTypeDeclaration declaration) throws SchemaServiceException {
		if (declaration == null) { return null; }
		final SecondaryType parent = constructSecondaryType(declaration.getParentName());
		final Queue<String> queue = new LinkedList<>();
		if (parent != null) {
			queue.add(parent.getName());
		}
		queue.addAll(declaration.getSecondaries());
		return null;
	}

	protected ObjectType constructType(TypeDeclaration type, Map<String, TypeDeclaration> typeHierarchy,
		Map<String, SecondaryTypeDeclaration> secondaries) throws SchemaServiceException {

		return null;
	}

	public boolean hasType(String name) {
		return false;
	}

	public boolean hasSecondaryType(String name) {
		return false;
	}
}