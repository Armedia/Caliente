package com.armedia.cmf.storage;

import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class CmfTypeMapper {

	public static final class TypeSpec implements Serializable, Comparable<TypeSpec> {
		private static final long serialVersionUID = 1L;

		private final CmfType baseType;
		private final String subType;

		private TypeSpec(CmfType baseType) {
			this(baseType, null);
		}

		private TypeSpec(CmfType baseType, String subType) {
			if (baseType == null) { throw new IllegalArgumentException("Must provide a valid base type"); }
			this.baseType = baseType;
			this.subType = Tools.coalesce(subType, baseType.name());
		}

		public CmfType getBaseType() {
			return this.baseType;
		}

		public String getSubType() {
			return this.subType;
		}

		@Override
		public int compareTo(TypeSpec o) {
			if (o == null) { return 1; }
			int r = Tools.compare(this.baseType, o.baseType);
			if (r != 0) { return r; }
			r = Tools.compare(this.subType, o.subType);
			return r;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.baseType, this.subType);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			TypeSpec other = TypeSpec.class.cast(obj);
			if (!Tools.equals(this.baseType, other.baseType)) { return false; }
			if (!Tools.equals(this.subType, other.subType)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("TypeSpec [baseType=%s, subType=%s]", this.baseType, this.subType);
		}
	}

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private boolean closed = false;

	protected final void configure(CfgTools cfg) throws Exception {
		boolean ok = false;
		this.lock.writeLock().lock();
		try {
			doConfigure(cfg);
			ok = true;
		} finally {
			if (!ok) {
				close();
			}
			this.closed = !ok;
			this.lock.writeLock().unlock();
		}
	}

	protected void doConfigure(CfgTools cfg) throws Exception {
		// Do nothing by default
	}

	protected final boolean isOpen() {
		this.lock.readLock().lock();
		try {
			return !this.closed;
		} finally {
			this.lock.readLock().unlock();
		}
	}

	/**
	 * <p>
	 * Constructs a new {@link TypeSpec} instance with the given data. The method
	 * {@link TypeSpec#getSubType()} will return the equivalent of {@code type.name()} (@see
	 * {@link Enum#name()}).
	 * </p>
	 *
	 * @param type
	 * @return a new {@link TypeSpec} instance with the given data
	 */
	protected final TypeSpec newTypeSpec(CmfType type) {
		return newTypeSpec(type, null);
	}

	/**
	 * <p>
	 * Constructs a new {@link TypeSpec} instance with the given data.
	 * </p>
	 *
	 * @param type
	 * @param subtype
	 * @return a new {@link TypeSpec} instance with the given data
	 */
	protected final TypeSpec newTypeSpec(CmfType type, String subtype) {
		return new TypeSpec(type, subtype);
	}

	/**
	 * <p>
	 * Identical to invoking {@link #mapType(CmfType, String)} with the {@code subType} parameter as
	 * {@code null}.
	 * </p>
	 *
	 * @param type
	 * @return the final mapping the source data translates to
	 */
	public final TypeSpec mapType(CmfType type) {
		return mapType(type, null);
	}

	/**
	 * <p>
	 * Check the mapping engine to translate the given mapping data into the desired mapping data.
	 * It will <b>never</b> return {@code null}. If no mapping is required, the returned
	 * {@link TypeSpec} will contain the same data the method was called with.
	 * </p>
	 *
	 * @param type
	 * @param subType
	 * @return the final mapping the source data translates to
	 */
	public final TypeSpec mapType(CmfType type, String subType) {
		TypeSpec src = new TypeSpec(type, subType);
		TypeSpec tgt = null;
		this.lock.readLock().lock();
		try {
			if (!isOpen()) { throw new IllegalStateException("This mapper has been closed"); }
			tgt = getMapping(src);
		} finally {
			this.lock.readLock().unlock();
		}
		if (tgt == null) {
			tgt = src;
		}
		return tgt;
	}

	/**
	 * <p>
	 * Performs the actual type mapping. Returns {@code null} if no mapping is to be performed.
	 * </p>
	 *
	 * @param sourceType
	 * @return the actual type mapping or {@code null} if no mapping is to be performed.
	 */
	protected abstract TypeSpec getMapping(TypeSpec sourceType);

	public final void close() {
		this.lock.writeLock().lock();
		try {
			if (!this.closed) {
				doClose();
			}
		} finally {
			this.closed = true;
			this.lock.writeLock().unlock();
		}
	}

	protected void doClose() {
		// Do nothing
	}

	public static CmfTypeMapper getTypeMapper(String name, CfgTools config) throws Exception {
		CmfTypeMapperFactory factory = CmfTypeMapperFactory.getFactory(name);
		if (factory == null) { return null; }
		return factory.getMapperInstance(config);
	}
}