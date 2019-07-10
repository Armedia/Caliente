/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.store;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

/**
 * <p>
 * This interface provides a mechanism through which object classes that interact with these CMS
 * classes can provide an attribute mapping service that permits obtaining "new" values of an
 * attribute, with respect to the original ones. One prime example of this is mapping the
 * "r_object_id" attribute from its source value to its target repository value.
 * </p>
 *
 *
 *
 */
public abstract class CmfValueMapper {

	/**
	 * <p>
	 * This class encapsulates a value mapping, including contextual information about what object
	 * type it's for, its name, and both values mapped.
	 * </p>
	 *
	 *
	 *
	 */
	public static final class Mapping {
		private final CmfObject.Archetype objectType;
		private final String mappingName;
		private final String sourceValue;
		private final String targetValue;

		private Mapping(CmfObject.Archetype objectType, String mappingName, String sourceValue, String targetValue) {
			if (objectType == null) { throw new IllegalArgumentException("Must provide an object type"); }
			if (mappingName == null) { throw new IllegalArgumentException("Must provide a mapping name"); }
			if (sourceValue == null) { throw new IllegalArgumentException("Must provide a source value to map from"); }
			if (targetValue == null) { throw new IllegalArgumentException("Must provide a target value to map to"); }
			this.objectType = objectType;
			this.mappingName = mappingName;
			this.sourceValue = sourceValue;
			this.targetValue = targetValue;
		}

		public CmfObject.Archetype getObjectType() {
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
			if (other == null) {
				throw new IllegalArgumentException("Must provide another mapping to compare against");
			}
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
			return String.format("Mapping [objectType=%s, mappingName=[%s], sourceValue=[%s], targetValue=[%s]]",
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
	protected final Mapping newMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue,
		String targetValue) {
		return new Mapping(objectType, mappingName, sourceValue, targetValue);
	}

	public final void clearMapping(Mapping oldMapping) {
		setMapping(oldMapping, null);
	}

	public final void clearTargetMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue) {
		setMapping(objectType, mappingName, sourceValue, null);
	}

	public final void clearSourceMapping(CmfObject.Archetype objectType, String mappingName, String targetValue) {
		setMapping(objectType, mappingName, null, targetValue);
	}

	public final void setMapping(Mapping oldMapping, String targetValue) {
		if (oldMapping == null) {
			throw new IllegalArgumentException("Must provide an old mapping to pattern the new mapping after");
		}
		setMapping(oldMapping.getObjectType(), oldMapping.getMappingName(), oldMapping.getSourceValue(), targetValue);
	}

	public final Mapping setMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue,
		String targetValue) {
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
	protected abstract Mapping createMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue,
		String targetValue);

	/**
	 * <p>
	 * Retrieves the value mapping for the given object type, name and source value, or {@code null}
	 * if no such mapping can be found. This operation is the reverse of
	 * {@link #getSourceMapping(CmfObject.Archetype, String, String)}.
	 * </p>
	 *
	 * @param objectType
	 * @param mappingName
	 * @param sourceValue
	 * @return the value mapping for the given object type, name and source value, or {@code null}
	 *         if no such mapping can be found.
	 */
	public abstract Mapping getTargetMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue);

	/**
	 * <p>
	 * Retrieves the value mapping for the given object type, name and target value, or {@code null}
	 * if no such mapping can be found. This operation is the reverse of
	 * {@link #getTargetMapping(CmfObject.Archetype, String, String)}
	 * </p>
	 *
	 * @param objectType
	 * @param mappingName
	 * @param targetValue
	 * @return the value mappings for the given object type, name and target values, or {@code null}
	 *         if no such mappings can be found.
	 */
	public abstract Collection<Mapping> getSourceMapping(CmfObject.Archetype objectType, String mappingName,
		String targetValue);

	/**
	 * <p>
	 * Retrieves all the available mappings in the system, per object type. Each object type will
	 * contain a set of mapping names that have been defined.
	 * </p>
	 *
	 * @return the mappings available in the system
	 */
	public abstract Map<CmfObject.Archetype, Set<String>> getAvailableMappings();

	/**
	 * <p>
	 * Retrieves the set of mapping names defined for a given object type in the system.
	 * </p>
	 *
	 * @param objectType
	 * @return the set of mapping names defined for a given object type in the system.
	 */
	public abstract Set<String> getAvailableMappings(CmfObject.Archetype objectType);

	/**
	 * <p>
	 * Retrieves the actual mappings for a given object type and mapping name as defined in the
	 * system. The key is the source mapping, and the value is the target mapping.
	 * </p>
	 *
	 * @param objectType
	 * @param mappingName
	 * @return the actual mappings for a given object type and mapping name as defined in the system
	 */
	public abstract Map<String, String> getMappings(CmfObject.Archetype objectType, String mappingName);
}