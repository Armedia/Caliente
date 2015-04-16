package com.armedia.cmf.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;

public abstract class DependencyManager<T, V> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected static final class Dependency {
		private final StoredObjectType type;
		private final String id;

		public Dependency(StoredObjectType type, String id) {
			if (id == null) { throw new IllegalArgumentException("Null ID not allowed"); }
			if (type == null) { throw new IllegalArgumentException("Null type not allowed"); }
			this.id = id;
			this.type = type;
		}

		public String getId() {
			return this.id;
		}

		public StoredObjectType getType() {
			return this.type;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.id, this.type);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			Dependency other = Dependency.class.cast(obj);
			if (!Tools.equals(this.id, other.id)) { return false; }
			if (this.type != other.type) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("Dependency [type=%s, id=%s]", this.type, this.id);
		}
	}

	public final boolean persistRelatedObject(T object, ObjectStorageTranslator<V> translator) throws StorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an T object"); }
		Boolean ret = persistObject(object, translator);
		if (ret == null) {
			final StoredObjectType type;
			try {
				type = translator.decodeObjectType(object);
			} catch (UnsupportedObjectTypeException e) {
				// Log a warning?
				this.log.warn(e.getMessage());
				return false;
			}
			try {
				return persistRelatedObject(type, translator.getObjectId(object));
			} catch (Exception e) {
				throw new StorageException("Failed to obtain the object's ID", e);
			}
		}
		return ret;
	}

	public final boolean persistRelatedObject(StoredObjectType type, String id) throws StorageException {
		final Dependency dep = new Dependency(type, id);
		boolean ret = persistDependency(dep);
		if (ret) {
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Registered %s", dep));
			}
		} else {
			// We use trace, not debug, to keep the noise down when we don't need it
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("Duplicate %s", dep));
			}
		}
		return ret;
	}

	/**
	 * <p>
	 * This method is analogous to {@link #persistDependency(Dependency)}, except that it affords
	 * the {@link DependencyManager} instance the opportunity to more deeply analyze the object, or
	 * perhaps even store it and avoid the need to load it again later. The return has the same
	 * meaning as the other method for the values {@link Boolean#TRUE} and {@link Boolean#FALSE},
	 * but adds a third meaning of {@code null} - to indicate that this method did nothing.
	 * </p>
	 *
	 * @param object
	 * @return same as {@link #persistDependency(Dependency)}, but with the added possibility of
	 *         {@code null} to indicate that the method did nothing.
	 * @throws StorageException
	 */
	protected Boolean persistObject(T object, ObjectStorageTranslator<V> translator) throws StorageException {
		return null;
	}

	/**
	 * <p>
	 * Register the given {@link Dependency}, and return {@code true} if the dependency is new and
	 * hasn't been registered yet, {@code false} if it has already been registered.
	 * </p>
	 *
	 * @param dependency
	 * @return {@code true} if the dependency is new and hasn't been registered yet, {@code false}
	 *         if it has already been registered
	 */
	protected abstract boolean persistDependency(Dependency dependency) throws StorageException;
}