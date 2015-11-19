/**
 *
 */

package com.armedia.cmf.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmfObject<V> {

	private final CmfType type;

	private final String id;
	private final String searchKey;
	private final String batchId;
	private final String label;
	private final String subtype;
	private final String productName;
	private final String productVersion;
	private final Map<String, CmfAttribute<V>> attributes = new HashMap<String, CmfAttribute<V>>();
	private final Map<String, CmfProperty<V>> properties = new HashMap<String, CmfProperty<V>>();
	private final CmfAttributeTranslator<V> translator;

	/**
	 * <p>
	 * Make a new copy of the object in the given pattern.
	 * </p>
	 *
	 * @param pattern
	 */
	public CmfObject(CmfObject<V> pattern) {
		this.type = pattern.getType();
		this.id = pattern.getId();
		this.searchKey = pattern.getSearchKey();
		this.batchId = pattern.getBatchId();
		this.label = pattern.getLabel();
		this.subtype = pattern.getSubtype();
		this.productName = pattern.getProductName();
		this.productVersion = pattern.getProductVersion();
		for (CmfAttribute<V> attribute : pattern.getAttributes()) {
			this.attributes.put(attribute.getName(), new CmfAttribute<V>(attribute));
		}
		for (CmfProperty<V> property : pattern.getProperties()) {
			this.properties.put(property.getName(), new CmfProperty<V>(property));
		}
		this.translator = pattern.translator;
	}

	/**
	 * <p>
	 * Make a new copy of the object in the given pattern, but with the given type specification
	 * data instead.
	 * </p>
	 *
	 * @param pattern
	 * @param altType
	 */
	CmfObject(CmfObject<V> pattern, String altSubType) {
		this.type = pattern.getType();
		this.subtype = altSubType;
		this.id = pattern.getId();
		this.searchKey = pattern.getSearchKey();
		this.batchId = pattern.getBatchId();
		this.label = pattern.getLabel();
		this.productName = pattern.getProductName();
		this.productVersion = pattern.getProductVersion();
		for (CmfAttribute<V> attribute : pattern.getAttributes()) {
			this.attributes.put(attribute.getName(), attribute);
		}
		for (CmfProperty<V> property : pattern.getProperties()) {
			this.properties.put(property.getName(), property);
		}
		this.translator = pattern.translator;
	}

	public CmfObject(CmfAttributeTranslator<V> translator, CmfType type, String id, String batchId, String label,
		String subtype, String productName, String productVersion) {
		this(translator, type, id, id, batchId, label, subtype, productName, productVersion);
	}

	public CmfObject(CmfAttributeTranslator<V> translator, CmfType type, String id, String searchKey, String batchId,
		String label, String subtype, String productName, String productVersion) {
		if (type == null) { throw new IllegalArgumentException("Must provide a valid object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide a valid object id"); }
		if (label == null) { throw new IllegalArgumentException("Must provide a valid object label"); }
		if (subtype == null) { throw new IllegalArgumentException("Must provide a valid object subtype"); }
		if (productName == null) { throw new IllegalArgumentException("Must provide a valid product name"); }
		if (productVersion == null) { throw new IllegalArgumentException("Must provide a valid product version"); }
		this.type = type;
		this.id = id;
		this.searchKey = Tools.coalesce(searchKey, id);
		this.batchId = Tools.coalesce(batchId, id);
		this.label = label;
		this.subtype = subtype;
		this.productName = productName;
		this.productVersion = productVersion;
		this.translator = translator;
	}

	public final CmfType getType() {
		return this.type;
	}

	public final String getSubtype() {
		return this.subtype;
	}

	public final String getId() {
		return this.id;
	}

	public final String getSearchKey() {
		return this.searchKey;
	}

	public final String getBatchId() {
		return this.batchId;
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
		return Collections.unmodifiableSet(this.attributes.keySet());
	}

	public final CmfAttribute<V> getAttribute(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return getAttribute(name.encode());
	}

	public final CmfAttribute<V> getAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return this.attributes.get(name);
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
		return Collections.unmodifiableCollection(this.attributes.values());
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
		return Collections.unmodifiableSet(this.properties.keySet());
	}

	public final CmfProperty<V> getProperty(CmfEncodeableName name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return getProperty(name.encode());
	}

	public final CmfProperty<V> getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return this.properties.get(name);
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
		return Collections.unmodifiableCollection(this.properties.values());
	}

	public final void setProperties(Collection<CmfProperty<V>> properties) {
		this.properties.clear();
		for (CmfProperty<V> prop : properties) {
			setProperty(prop);
		}
	}

	public final CmfObject<CmfValue> getEncodedVersion() throws CmfValueEncoderException {
		if (this.translator == null) { return null; }
		CmfObject<CmfValue> encoded = new CmfObject<CmfValue>(null, this.type, this.id, this.searchKey, this.batchId,
			this.label, this.subtype, this.productName, this.productVersion);
		for (CmfAttribute<V> att : this.attributes.values()) {
			final CmfValueCodec<V> codec = this.translator.getCodec(att.getType());
			CmfAttribute<CmfValue> newAtt = new CmfAttribute<CmfValue>(this.translator.encodeAttributeName(this.type,
				att.getName()), att.getType(), att.isRepeating());
			for (V v : att) {
				newAtt.addValue(codec.encodeValue(v));
			}
			encoded.setAttribute(newAtt);
		}
		for (CmfProperty<V> prop : this.properties.values()) {
			final CmfValueCodec<V> codec = this.translator.getCodec(prop.getType());
			CmfProperty<CmfValue> newProp = new CmfProperty<CmfValue>(this.translator.encodeAttributeName(this.type,
				prop.getName()), prop.getType(), prop.isRepeating());
			for (V v : prop) {
				newProp.addValue(codec.encodeValue(v));
			}
			encoded.setProperty(newProp);
		}
		return encoded;
	}

	protected String toStringTrailer() {
		return "";
	}

	@Override
	public final String toString() {
		final String trailer = toStringTrailer();
		final String trailerSep = ((trailer != null) && (trailer.length() > 0) ? ", " : "");
		return String.format("%s [type=%s, subtype=%s, id=%s, searchKey=%s, batchId=%s, label=%s%s%s]", getClass()
			.getSimpleName(), this.type, this.subtype, this.id, this.searchKey, this.batchId, this.label, trailerSep,
			trailer);
	}
}