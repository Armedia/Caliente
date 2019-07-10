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
/**
 *
 */

package com.armedia.caliente.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.LazySupplier;

/**
 *
 *
 */
public class CmfObject<VALUE> extends CmfObjectSearchSpec implements Iterable<CmfAttribute<VALUE>> {
	private static final long serialVersionUID = 1L;

	public static enum Archetype {
		//
		DATASTORE("ds"), //
		USER("usr"), //
		GROUP("grp"), //
		ACL, //
		TYPE("typ"), //
		FORMAT("fmt"), //
		FOLDER("fld"), //
		DOCUMENT("doc"), //
		// RELATION("rel"), //
		//
		;

		public final String abbrev;

		private Archetype() {
			this(null);
		}

		private Archetype(String abbreviation) {
			this.abbrev = StringUtils.lowerCase(Tools.coalesce(abbreviation, name()));
		}

		private static final Map<String, CmfObject.Archetype> ABBREV;
		private static final Set<String> NAMES;
		static {
			Map<String, CmfObject.Archetype> abb = new TreeMap<>();
			Set<String> n = new TreeSet<>();
			for (CmfObject.Archetype t : CmfObject.Archetype.values()) {
				n.add(t.name());
				CmfObject.Archetype o = abb.put(t.abbrev, t);
				if (o != null) {
					throw new RuntimeException(String.format(
						"ERROR: The CmfObject.Archetype values %s and %s share the same abbreviation [%s]", t.name(),
						o.name(), t.abbrev));
				}
			}
			NAMES = Tools.freezeSet(new LinkedHashSet<>(n));
			ABBREV = Tools.freezeMap(new LinkedHashMap<>(abb));
		}

		public static Set<String> getNames() {
			return Archetype.NAMES;
		}

		public static CmfObject.Archetype decode(String value) {
			if (value == null) { return null; }
			try {
				return CmfObject.Archetype.valueOf(StringUtils.upperCase(value));
			} catch (final IllegalArgumentException e) {
				// Maybe an abbreviation?
				CmfObject.Archetype t = Archetype.ABBREV.get(StringUtils.lowerCase(value));
				if (t != null) { return t; }
				throw e;
			}
		}
	}

	private Long number = null;
	private final String name;
	private final Collection<CmfObjectRef> parentIds;
	private final int dependencyTier;
	private final String historyId;
	private final boolean historyCurrent;
	private final String label;
	private final String subtype;
	private final Set<String> secondaries;
	private final Map<String, CmfAttribute<VALUE>> attributes = new HashMap<>();
	private final Map<String, CmfProperty<VALUE>> properties = new HashMap<>();
	private final CmfAttributeTranslator<VALUE> translator;
	private final LazySupplier<String> description = new LazySupplier<>(this::renderDescription);
	private final LazySupplier<String> string = new LazySupplier<>(this::renderString);

	/**
	 * <p>
	 * Make a new copy of the object in the given pattern.
	 * </p>
	 *
	 * @param pattern
	 */
	public CmfObject(CmfObject<VALUE> pattern) {
		super(pattern);
		this.number = pattern.getNumber();
		this.name = pattern.getName();
		this.parentIds = new ArrayList<>(pattern.parentIds);
		this.dependencyTier = pattern.getDependencyTier();
		this.historyId = pattern.getHistoryId();
		this.historyCurrent = pattern.isHistoryCurrent();
		this.label = pattern.getLabel();
		this.subtype = pattern.getSubtype();
		for (CmfAttribute<VALUE> attribute : pattern.getAttributes()) {
			this.attributes.put(attribute.getName(), new CmfAttribute<>(attribute));
		}
		for (CmfProperty<VALUE> property : pattern.getProperties()) {
			this.properties.put(property.getName(), new CmfProperty<>(property));
		}
		this.secondaries = Tools.freezeSet(new LinkedHashSet<>(pattern.getSecondarySubtypes()));
		this.translator = pattern.translator;
	}

	public CmfObject(CmfAttributeTranslator<VALUE> translator, CmfObject.Archetype type, String id, String name,
		Collection<CmfObjectRef> parentIds, int dependencyTier, String historyId, boolean historyCurrent, String label,
		String subtype, Set<String> secondaries, Long number) {
		this(translator, type, id, name, parentIds, id, dependencyTier, historyId, historyCurrent, label, subtype,
			secondaries, number);
	}

	public CmfObject(CmfAttributeTranslator<VALUE> translator, CmfObject.Archetype type, String id, String name,
		Collection<CmfObjectRef> parentIds, String searchKey, int dependencyTier, String historyId,
		boolean historyCurrent, String label, String subtype, Set<String> secondaries, Long number) {
		super(type, id, searchKey);
		if (translator == null) { throw new IllegalArgumentException("Must provide a valid value translator"); }
		if (type == null) { throw new IllegalArgumentException("Must provide a valid object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide a valid object id"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a valid object id"); }
		if (label == null) { throw new IllegalArgumentException("Must provide a valid object label"); }
		if (subtype == null) { throw new IllegalArgumentException("Must provide a valid object subtype"); }
		if (parentIds == null) {
			parentIds = Collections.emptyList();
		}
		this.number = number;
		this.name = name;
		this.parentIds = Tools.freezeCollection(new ArrayList<>(parentIds));
		this.dependencyTier = dependencyTier;
		this.historyId = Tools.coalesce(historyId, id);
		this.historyCurrent = (historyId == null ? true : historyCurrent);
		this.label = label;
		this.subtype = subtype;
		this.secondaries = Tools.freezeSet(new LinkedHashSet<>(secondaries));
		this.translator = translator;
	}

	public final CmfAttributeTranslator<VALUE> getTranslator() {
		return this.translator;
	}

	final void setNumber(Long number) {
		if (number == null) { throw new IllegalArgumentException("Must provide a number to set"); }
		if (this.number != null) { throw new IllegalStateException("A number has already been set, can't change it"); }
		this.number = number;
	}

	public final void copyNumber(CmfObject<?> other) {
		Objects.requireNonNull(other, "Must provide an object whose number to copy");
		if (!new CmfObjectSearchSpec(this).equals(new CmfObjectSearchSpec(other))) {
			throw new IllegalArgumentException(
				String.format("The given %s is not the same as %s", other.getDescription(), this.getDescription()));
		}
		if (this == other) { return; }
		setNumber(other.getNumber());
	}

	public final String getName() {
		return this.name;
	}

	public final Collection<CmfObjectRef> getParentReferences() {
		return this.parentIds;
	}

	public final Long getNumber() {
		return this.number;
	}

	public final String getSubtype() {
		return this.subtype;
	}

	public final int getDependencyTier() {
		return this.dependencyTier;
	}

	public final String getHistoryId() {
		return this.historyId;
	}

	public final boolean isHistoryCurrent() {
		return this.historyCurrent;
	}

	public final String getLabel() {
		return this.label;
	}

	public final int getAttributeCount() {
		return this.attributes.size();
	}

	public final Set<String> getAttributeNames() {
		return new HashSet<>(this.attributes.keySet());
	}

	public final Set<String> getSecondarySubtypes() {
		return this.secondaries;
	}

	public final CmfAttribute<VALUE> getAttribute(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return getAttribute(name.encode());
	}

	public final CmfAttribute<VALUE> getAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return this.attributes.get(name);
	}

	public final boolean hasAttribute(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to check for"); }
		return hasAttribute(name.encode());
	}

	public final boolean hasAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to check for"); }
		return this.attributes.containsKey(name);
	}

	public final CmfAttribute<VALUE> setAttribute(CmfAttribute<VALUE> attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set"); }
		return this.attributes.put(attribute.getName(), attribute);
	}

	public final CmfAttribute<VALUE> removeAttribute(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return removeAttribute(name.encode());
	}

	public final CmfAttribute<VALUE> removeAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return this.attributes.remove(name);
	}

	public final Collection<CmfAttribute<VALUE>> getAttributes() {
		return new ArrayList<>(this.attributes.values());
	}

	public final void setAttributes(Collection<CmfAttribute<VALUE>> attributes) {
		this.attributes.clear();
		for (CmfAttribute<VALUE> att : attributes) {
			setAttribute(att);
		}
	}

	@Override
	public Iterator<CmfAttribute<VALUE>> iterator() {
		return this.attributes.values().iterator();
	}

	public final int getPropertyCount() {
		return this.properties.size();
	}

	public final Set<String> getPropertyNames() {
		return new HashSet<>(this.properties.keySet());
	}

	public final CmfProperty<VALUE> getProperty(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return getProperty(name.encode());
	}

	public final CmfProperty<VALUE> getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return this.properties.get(name);
	}

	public final boolean hasProperty(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to check for"); }
		return hasProperty(name.encode());
	}

	public final boolean hasProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to check for"); }
		return this.properties.containsKey(name);
	}

	public final CmfProperty<VALUE> setProperty(CmfProperty<VALUE> property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a property to set"); }
		return this.properties.put(property.getName(), property);
	}

	public final CmfProperty<VALUE> removeProperty(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return removeProperty(name.encode());
	}

	public final CmfProperty<VALUE> removeProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return this.properties.remove(name);
	}

	public final Collection<CmfProperty<VALUE>> getProperties() {
		return new ArrayList<>(this.properties.values());
	}

	public final void setProperties(Collection<CmfProperty<VALUE>> properties) {
		this.properties.clear();
		for (CmfProperty<VALUE> prop : properties) {
			setProperty(prop);
		}
	}

	protected String toStringTrailer() {
		return "";
	}

	public final String getDescription() {
		return this.description.get();
	}

	private String renderDescription() {
		return String.format("%s [%s](%s)", getType().name(), this.label, getId());
	}

	@Override
	public final String toString() {
		return this.string.get();
	}

	private String renderString() {
		final String trailer = toStringTrailer();
		final String trailerSep = ((trailer != null) && (trailer.length() > 0) ? ", " : "");
		return String.format(
			"%s [type=%s, subtype=%s, secondaries=%s, id=%s, name=%s, searchKey=%s, dependencyTier=%d, historyId=%s, historyCurrent=%s, label=%s%s%s]",
			getClass().getSimpleName(), getType(), this.subtype, this.secondaries, getId(), this.name, getSearchKey(),
			this.dependencyTier, this.historyId, this.historyCurrent, this.label, trailerSep, trailer);
	}
}