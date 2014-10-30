package com.delta.cmsmf.cms;

import org.apache.log4j.Logger;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.UnsupportedObjectTypeException;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public abstract class DctmDependencyManager {

	protected final Logger log = Logger.getLogger(getClass());

	protected static final class Dependency {
		private final DctmObjectType type;
		private final String id;

		public Dependency(IDfPersistentObject obj) throws DfException, UnsupportedObjectTypeException {
			this(DctmObjectType.decodeType(obj), obj.getObjectId());
		}

		public Dependency(DctmPersistentObject<?> obj) {
			this(obj.getDctmType(), obj.getId());
		}

		public Dependency(DctmObjectType type, IDfId id) {
			this(type, id.getId());
		}

		public Dependency(DctmObjectType type, String id) {
			if (id == null) { throw new IllegalArgumentException("Null ID not allowed"); }
			if (type == null) { throw new IllegalArgumentException("Null type not allowed"); }
			this.id = id;
			this.type = type;
		}

		public String getId() {
			return this.id;
		}

		public DctmObjectType getType() {
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

	public final boolean persistRelatedObject(IDfPersistentObject object) throws DfException, CMSMFException {
		if (object == null) { throw new IllegalArgumentException("Must provide an IDfPersistentObject object"); }
		Boolean ret = persistDfObject(object);
		if (ret == null) {
			final DctmObjectType type;
			try {
				type = DctmObjectType.decodeType(object);
			} catch (UnsupportedObjectTypeException e) {
				// Log a warning?
				this.log.warn(e.getMessage());
				return false;
			}
			return persistRelatedObject(type, object.getObjectId());
		}
		return ret;
	}

	public final boolean persistRelatedObject(DctmObjectType type, IDfId id) throws CMSMFException {
		if (type == null) { throw new IllegalArgumentException("Must provide a dependency type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide dependency id"); }
		return persistRelatedObject(type, id.getId());
	}

	public final boolean persistRelatedObject(DctmObjectType type, String id) throws CMSMFException {
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
	 * the {@link DctmDependencyManager} instance the opportunity to more deeply analyze the object,
	 * or perhaps even store it and avoid the need to load it again later. The return has the same
	 * meaning as the other method for the values {@link Boolean#TRUE} and {@link Boolean#FALSE},
	 * but adds a third meaning of {@code null} - to indicate that this method did nothing.
	 * </p>
	 *
	 * @param object
	 * @return same as {@link #persistDependency(Dependency)}, but with the added possibility of
	 *         {@code null} to indicate that the method did nothing.
	 * @throws DfException
	 * @throws CMSMFException
	 */
	protected Boolean persistDfObject(IDfPersistentObject object) throws DfException, CMSMFException {
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
	protected abstract boolean persistDependency(Dependency dependency) throws CMSMFException;
}