package com.armedia.caliente.engine.transform;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public abstract class TransformableObject {

	protected final Map<String, TypedValue> attributes = new TreeMap<>();
	protected final Map<String, TypedValue> privateProperties = new TreeMap<>();

	private String subtype = null;
	private String name = null;

	public abstract String getObjectId();

	public abstract String getHistoryId();

	public abstract boolean isHistoryCurrent();

	public abstract CmfType getType();

	public abstract String getLabel();

	public abstract String getOriginalSubtype();

	public String getSubtype() {
		return Tools.coalesce(this.subtype, getOriginalSubtype());
	}

	public TransformableObject setSubtype(String subtype) {
		this.subtype = subtype;
		return this;
	}

	public String getName() {
		return Tools.coalesce(this.name, getOriginalName());
	}

	public TransformableObject setName(String name) {
		this.name = name;
		return this;
	}

	public abstract Set<String> getOriginalDecorators();

	public abstract Set<String> getDecorators();

	public abstract int getDependencyTier();

	public abstract String getOriginalName();

	public abstract String getProductName();

	public abstract String getProductVersion();

	public Map<String, TypedValue> getAtt() {
		return this.attributes;
	}

	public Map<String, TypedValue> getPriv() {
		return this.privateProperties;
	}

	public CmfObject<CmfValue> applyChanges(CmfObject<CmfValue> object) throws TransformationException {
		CmfObject<CmfValue> newObject = new CmfObject<>(//
			object.getTranslator(), //
			getType(), //
			getObjectId(), //
			getName(), //
			object.getParentReferences(), //
			getDependencyTier(), //
			getHistoryId(), //
			isHistoryCurrent(), //
			getLabel(), //
			getSubtype(), //
			getProductName(), //
			getProductVersion(), //
			object.getNumber() //
		);
		// Create the list of attributes to copy...
		Collection<CmfAttribute<CmfValue>> attributeList = new ArrayList<>(this.attributes.size());
		for (String s : this.attributes.keySet()) {
			TypedValue v = this.attributes.get(s);
			CmfAttribute<CmfValue> a = new CmfAttribute<>(v.getName(), v.getType(), v.isRepeating());
			for (Object o : v.getValues()) {
				try {
					a.addValue(new CmfValue(v.getType(), o));
				} catch (ParseException e) {
					throw new TransformationException(
						String.format("Failed to convert the %s value [%s] into a %s for attribute [%s]",
							o.getClass().getCanonicalName(), o, v.getType(), v.getName()),
						e);
				}
			}
			attributeList.add(a);
		}

		// Create the list of properties to copy...
		Collection<CmfProperty<CmfValue>> propertyList = new ArrayList<>(this.privateProperties.size());
		for (String s : this.privateProperties.keySet()) {
			TypedValue v = this.privateProperties.get(s);
			CmfProperty<CmfValue> p = new CmfProperty<>(v.getName(), v.getType(), v.isRepeating());
			for (Object o : v.getValues()) {
				try {
					p.addValue(new CmfValue(v.getType(), o));
				} catch (ParseException e) {
					throw new TransformationException(
						String.format("Failed to convert the %s value [%s] into a %s for property [%s]",
							o.getClass().getCanonicalName(), o, v.getType(), v.getName()),
						e);
				}
			}
			propertyList.add(p);
		}

		// Do the actual copying...
		newObject.setAttributes(attributeList);
		newObject.setProperties(propertyList);

		// TODO: The aspects (decorators).... need a mechanism to set these...

		return newObject;
	}
}