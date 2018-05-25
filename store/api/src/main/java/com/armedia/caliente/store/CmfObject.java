/**
 *
 */

package com.armedia.caliente.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmfObject<V> extends CmfObjectSearchSpec {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	private Long number = null;
	private final String name;
	private final Collection<CmfObjectRef> parentIds;
	private final int dependencyTier;
	private final String historyId;
	private final boolean historyCurrent;
	private final String label;
	private final String subtype;
	private final Set<String> secondaries;
	private final String productName;
	private final String productVersion;
	private final Map<String, CmfAttribute<V>> attributes = new HashMap<>();
	private final Map<String, CmfProperty<V>> properties = new HashMap<>();
	private final CmfAttributeTranslator<V> translator;

	/**
	 * <p>
	 * Make a new copy of the object in the given pattern.
	 * </p>
	 *
	 * @param pattern
	 */
	public CmfObject(CmfObject<V> pattern) {
		super(pattern);
		this.number = pattern.getNumber();
		this.name = pattern.getName();
		this.parentIds = new ArrayList<>(pattern.parentIds);
		this.dependencyTier = pattern.getDependencyTier();
		this.historyId = pattern.getHistoryId();
		this.historyCurrent = pattern.isHistoryCurrent();
		this.label = pattern.getLabel();
		this.subtype = pattern.getSubtype();
		this.productName = pattern.getProductName();
		this.productVersion = pattern.getProductVersion();
		for (CmfAttribute<V> attribute : pattern.getAttributes()) {
			this.attributes.put(attribute.getName(), new CmfAttribute<>(attribute));
		}
		for (CmfProperty<V> property : pattern.getProperties()) {
			this.properties.put(property.getName(), new CmfProperty<>(property));
		}
		this.secondaries = Tools.freezeSet(new LinkedHashSet<>(pattern.getSecondarySubtypes()));
		this.translator = pattern.translator;
	}

	public CmfObject(CmfAttributeTranslator<V> translator, CmfType type, String id, String name,
		Collection<CmfObjectRef> parentIds, int dependencyTier, String historyId, boolean historyCurrent, String label,
		String subtype, Set<String> secondaries, String productName, String productVersion, Long number) {
		this(translator, type, id, name, parentIds, id, dependencyTier, historyId, historyCurrent, label, subtype,
			secondaries, productName, productVersion, number);
	}

	public CmfObject(CmfAttributeTranslator<V> translator, CmfType type, String id, String name,
		Collection<CmfObjectRef> parentIds, String searchKey, int dependencyTier, String historyId,
		boolean historyCurrent, String label, String subtype, Set<String> secondaries, String productName,
		String productVersion, Long number) {
		super(type, id, searchKey);
		if (translator == null) { throw new IllegalArgumentException("Must provide a valid value translator"); }
		if (type == null) { throw new IllegalArgumentException("Must provide a valid object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide a valid object id"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a valid object id"); }
		if (label == null) { throw new IllegalArgumentException("Must provide a valid object label"); }
		if (subtype == null) { throw new IllegalArgumentException("Must provide a valid object subtype"); }
		if (productName == null) { throw new IllegalArgumentException("Must provide a valid product name"); }
		if (productVersion == null) { throw new IllegalArgumentException("Must provide a valid product version"); }
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
		this.productName = productName;
		this.productVersion = productVersion;
		this.translator = translator;
	}

	public final CmfAttributeTranslator<V> getTranslator() {
		return this.translator;
	}

	final void setNumber(Long number) {
		if (number == null) { throw new IllegalArgumentException("Must provide a number to set"); }
		if (this.number != null) { throw new IllegalStateException("A number has already been set, can't change it"); }
		this.number = number;
	}

	public final void copyNumber(CmfObject<?> other) {
		Objects.requireNonNull(other, "Must provide an object whose number to copy");
		if (!new CmfObjectSearchSpec(this).equals(new CmfObjectSearchSpec(other))) { throw new IllegalArgumentException(
			String.format("The given %s is not the same as %s", other.getDescription(), this.getDescription())); }
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

	public final String getProductName() {
		return this.productName;
	}

	public final String getProductVersion() {
		return this.productVersion;
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

	public final CmfAttribute<V> getAttribute(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return getAttribute(name.encode());
	}

	public final CmfAttribute<V> getAttribute(String name) {
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

	public final CmfAttribute<V> setAttribute(CmfAttribute<V> attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set"); }
		return this.attributes.put(attribute.getName(), attribute);
	}

	public final CmfAttribute<V> removeAttribute(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return removeAttribute(name.encode());
	}

	public final CmfAttribute<V> removeAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return this.attributes.remove(name);
	}

	public final Collection<CmfAttribute<V>> getAttributes() {
		return new ArrayList<>(this.attributes.values());
	}

	public final void setAttributes(Collection<CmfAttribute<V>> attributes) {
		this.attributes.clear();
		for (CmfAttribute<V> att : attributes) {
			setAttribute(att);
		}
	}

	public final int getPropertyCount() {
		return this.properties.size();
	}

	public final Set<String> getPropertyNames() {
		return new HashSet<>(this.properties.keySet());
	}

	public final CmfProperty<V> getProperty(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return getProperty(name.encode());
	}

	public final CmfProperty<V> getProperty(String name) {
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

	public final CmfProperty<V> setProperty(CmfProperty<V> property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a property to set"); }
		return this.properties.put(property.getName(), property);
	}

	public final CmfProperty<V> removeProperty(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return removeProperty(name.encode());
	}

	public final CmfProperty<V> removeProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return this.properties.remove(name);
	}

	public final Collection<CmfProperty<V>> getProperties() {
		return new ArrayList<>(this.properties.values());
	}

	public final void setProperties(Collection<CmfProperty<V>> properties) {
		this.properties.clear();
		for (CmfProperty<V> prop : properties) {
			setProperty(prop);
		}
	}

	protected String toStringTrailer() {
		return "";
	}

	public final String getDescription() {
		return String.format("%s [%s](%s)", getType().name(), this.label, getId());
	}

	@Override
	public final String toString() {
		final String trailer = toStringTrailer();
		final String trailerSep = ((trailer != null) && (trailer.length() > 0) ? ", " : "");
		return String.format(
			"%s [type=%s, subtype=%s, secondaries=%s, id=%s, name=%s, searchKey=%s, dependencyTier=%d, historyId=%s, historyCurrent=%s, label=%s%s%s]",
			getClass().getSimpleName(), getType(), this.subtype, this.secondaries, getId(), this.name, getSearchKey(),
			this.dependencyTier, this.historyId, this.historyCurrent, this.label, trailerSep, trailer);
	}
}