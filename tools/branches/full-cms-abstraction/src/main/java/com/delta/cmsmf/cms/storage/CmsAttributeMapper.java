package com.delta.cmsmf.cms.storage;

import com.armedia.commons.utilities.Tools;

/**
 * <p>
 * This interface provides a mechanism through which object classes that interact with these CMS
 * classes can provide an attribute mapping service that permits obtaining "new" values of an
 * attribute, with respect to the original ones. One prime example of this is mapping the
 * "r_object_id" attribute from its source value to its target repository value.
 * </p>
 *
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class CmsAttributeMapper {

	/**
	 * <p>
	 * This class encapsulates a value mapping, including contextual information about what object
	 * type it's for, its name, and both values mapped.
	 * </p>
	 *
	 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
	 *
	 */
	public static final class Mapping {
		private final CmsObjectType objectType;
		private final String mappingName;
		private final String sourceValue;
		private final String targetValue;

		private Mapping(CmsObjectType objectType, String mappingName, String sourceValue, String targetValue) {
			if (objectType == null) { throw new IllegalArgumentException("Must provide an object type"); }
			if (mappingName == null) { throw new IllegalArgumentException("Must provide a mapping name"); }
			if (sourceValue == null) { throw new IllegalArgumentException("Must provide a source value to map from"); }
			if (targetValue == null) { throw new IllegalArgumentException("Must provide a target value to map to"); }
			this.objectType = objectType;
			this.mappingName = mappingName;
			this.sourceValue = sourceValue;
			this.targetValue = targetValue;
		}

		public CmsObjectType getObjectType() {
			return this.objectType;
		}

		public String getMappingName() {
			return this.mappingName;
		}

		public String getSourceValue() {
			return this.sourceValue;
		}

		public String getTargetValue() {
			return this.targetValue;
		}

		public boolean isSameTypeAndName(Mapping other) {
			if (other == null) { throw new IllegalArgumentException("Must provide another mapping to compare against"); }
			if (!Tools.equals(this.objectType, other.objectType)) { return false; }
			if (!Tools.equals(this.mappingName, other.mappingName)) { return false; }
			return true;
		}

		public boolean isSameSource(Mapping other) {
			if (!isSameTypeAndName(other)) { return false; }
			if (!Tools.equals(this.sourceValue, other.sourceValue)) { return false; }
			return true;
		}

		public boolean isSameTarget(Mapping other) {
			if (!isSameTypeAndName(other)) { return false; }
			if (!Tools.equals(this.targetValue, other.targetValue)) { return false; }
			return true;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.objectType, this.mappingName, this.sourceValue, this.targetValue);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			Mapping other = Mapping.class.cast(obj);
			if (!Tools.equals(this.objectType, other.objectType)) { return false; }
			if (!Tools.equals(this.mappingName, other.mappingName)) { return false; }
			if (!Tools.equals(this.sourceValue, other.sourceValue)) { return false; }
			if (!Tools.equals(this.targetValue, other.targetValue)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("Mapping [objectType=%s, mappingName=%s, sourceValue=%s, targetValue=%s]",
				this.objectType, this.mappingName, this.sourceValue, this.targetValue);
		}
	}

	/**
	 * <p>
	 * Creates a new instance of {@link Mapping}
	 * </p>
	 *
	 * @param objectType
	 * @param mappingName
	 * @param sourceValue
	 * @param targetValue
	 * @return a new instance of {@link Mapping}
	 */
	protected final Mapping newMapping(CmsObjectType objectType, String mappingName, String sourceValue,
		String targetValue) {
		return new Mapping(objectType, mappingName, sourceValue, targetValue);
	}

	public final void clearMapping(Mapping oldMapping) {
		setMapping(oldMapping, null);
	}

	public final void clearTargetMapping(CmsObjectType objectType, String mappingName, String sourceValue) {
		setMapping(objectType, mappingName, sourceValue, null);
	}

	public final void clearSourceMapping(CmsObjectType objectType, String mappingName, String targetValue) {
		setMapping(objectType, mappingName, null, targetValue);
	}

	public final void setMapping(Mapping oldMapping, String targetValue) {
		if (oldMapping == null) { throw new IllegalArgumentException(
			"Must provide an old mapping to pattern the new mapping after"); }
		setMapping(oldMapping.getObjectType(), oldMapping.getMappingName(), oldMapping.getSourceValue(), targetValue);
	}

	public final Mapping setMapping(CmsObjectType objectType, String mappingName, String sourceValue, String targetValue) {
		if (objectType == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (mappingName == null) { throw new IllegalArgumentException("Must provide a mapping name"); }
		return createMapping(objectType, mappingName, sourceValue, targetValue);
	}

	/**
	 * <p>
	 * Creates a new mapping for the given object type, name and source value that will resolve to
	 * the target value. If the either the source or target values are {@code null}, then that means
	 * that the mapping should be cleared completely, using the non-{@code null} value as a
	 * reference (i.e. if the target value is {@code null}, then search based on the source value,
	 * and vice-versa). If both values are {@code null}, then the mapping can't be cleared and an
	 * {@link IllegalArgumentException} should be raised.
	 * </p>
	 *
	 * @param objectType
	 * @param mappingName
	 * @param sourceValue
	 * @param targetValue
	 * @return the mapping created, or {@code null} if exactly one of {@code sourceValue} or
	 *         {@code targetValue} was {@code null}
	 * @throws IllegalArgumentException
	 *             if both {@code sourceValue} and {@code targetValue} were {@code null}
	 */
	protected abstract Mapping createMapping(CmsObjectType objectType, String mappingName, String sourceValue,
		String targetValue);

	/**
	 * <p>
	 * Retrieves the value mapping for the given object type, name and source value, or {@code null}
	 * if no such mapping can be found. This operation is the reverse of
	 * {@link #getSourceMapping(CmsObjectType, String, String)}.
	 * </p>
	 *
	 * @param objectType
	 * @param mappingName
	 * @param sourceValue
	 * @return the value mapping for the given object type, name and source value, or {@code null}
	 *         if no such mapping can be found.
	 */
	public abstract Mapping getTargetMapping(CmsObjectType objectType, String mappingName, String sourceValue);

	/**
	 * <p>
	 * Retrieves the value mapping for the given object type, name and target value, or {@code null}
	 * if no such mapping can be found. This operation is the reverse of
	 * {@link #getTargetMapping(CmsObjectType, String, String)}
	 * </p>
	 *
	 * @param objectType
	 * @param mappingName
	 * @param targetValue
	 * @return the value mapping for the given object type, name and source value, or {@code null}
	 *         if no such mapping can be found.
	 */
	public abstract Mapping getSourceMapping(CmsObjectType objectType, String mappingName, String targetValue);
}