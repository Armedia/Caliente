package com.delta.cmsmf.cms;

import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public abstract class CmsDependencyManager {

	protected final Logger log = Logger.getLogger(getClass());

	protected static final class Dependency {
		private final CmsObjectType type;
		private final String id;

		protected Dependency(IDfPersistentObject obj) throws DfException {
			this(CmsObjectType.decodeType(obj), obj.getObjectId());
		}

		protected Dependency(CmsObjectType type, IDfId id) {
			this(type, id.getId());
		}

		protected Dependency(CmsObjectType type, String id) {
			if (id == null) { throw new IllegalArgumentException("Null ID not allowed"); }
			if (type == null) { throw new IllegalArgumentException("Null type not allowed"); }
			this.id = id;
			this.type = type;
		}

		public String getId() {
			return this.id;
		}

		public CmsObjectType getType() {
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

	public final void registerDependency(IDfPersistentObject object) throws DfException, CMSMFException {
		if (object == null) { throw new IllegalArgumentException("Must provide a dependency object"); }
		registerDependency(CmsObjectType.decodeType(object), object.getObjectId());
	}

	public final void registerDependency(CmsObjectType type, IDfId id) throws CMSMFException {
		if (type == null) { throw new IllegalArgumentException("Must provide a dependency type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide dependency id"); }
		registerDependency(type, id.getId());
	}

	public final void registerDependency(CmsObjectType type, String id) throws CMSMFException {
		final Dependency dep = new Dependency(type, id);
		if (registerDependency(dep)) {
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Registered %s", dep));
			}
		} else {
			// We use trace, not debug, to keep the noise down when we don't need it
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("Duplicate %s", dep));
			}
		}
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
	protected abstract boolean registerDependency(Dependency dependency) throws CMSMFException;
}