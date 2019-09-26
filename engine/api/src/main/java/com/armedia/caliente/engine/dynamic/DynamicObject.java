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
package com.armedia.caliente.engine.dynamic;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.caliente.engine.dynamic.transformer.TransformerException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.commons.utilities.Tools;

public abstract class DynamicObject implements MutableScriptableObjectFacade<DynamicValue> {

	protected final Map<String, DynamicValue> attributes = new TreeMap<>();
	protected final Map<String, DynamicValue> privateProperties = new TreeMap<>();

	private String subtype = null;
	private String name = null;

	@Override
	public abstract String getObjectId();

	@Override
	public abstract String getHistoryId();

	@Override
	public abstract boolean isHistoryCurrent();

	@Override
	public abstract CmfObject.Archetype getType();

	@Override
	public abstract String getLabel();

	@Override
	public abstract String getOriginalSubtype();

	@Override
	public String getSubtype() {
		return Tools.coalesce(this.subtype, getOriginalSubtype());
	}

	@Override
	public DynamicObject setSubtype(String subtype) {
		this.subtype = subtype;
		return this;
	}

	@Override
	public String getName() {
		return Tools.coalesce(this.name, getOriginalName());
	}

	@Override
	public DynamicObject setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public abstract Set<String> getOriginalSecondarySubtypes();

	@Override
	public abstract Set<String> getSecondarySubtypes();

	@Override
	public abstract int getDependencyTier();

	@Override
	public abstract String getOriginalName();

	@Override
	public Map<String, DynamicValue> getAtt() {
		return this.attributes;
	}

	@Override
	public Map<String, DynamicValue> getPriv() {
		return this.privateProperties;
	}

	public <VALUE> CmfObject<VALUE> applyChanges(CmfObject<VALUE> object) throws TransformerException {
		CmfObject<VALUE> newObject = new CmfObject<>(//
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
			getSecondarySubtypes(), //
			object.getNumber() //
		);
		// Create the list of attributes to copy...
		CmfAttributeTranslator<VALUE> translator = object.getTranslator();
		for (String s : this.attributes.keySet()) {
			DynamicValue v = this.attributes.get(s);
			CmfAttribute<VALUE> newAttribute = new CmfAttribute<>(v.getName(), v.getType(), v.isMultivalued());
			if (newAttribute.isMultivalued()) {
				for (Object o : v.getValues()) {
					try {
						newAttribute.addValue(translator.getValue(v.getType(), o));
					} catch (ParseException e) {
						throw new TransformerException(
							String.format("Failed to convert the %s value [%s] into a %s for attribute [%s]",
								o.getClass().getCanonicalName(), o, v.getType(), v.getName()),
							e);
					}
				}
			} else {
				Object o = v.getValue();
				try {
					newAttribute.setValue(translator.getValue(v.getType(), o));
				} catch (ParseException e) {
					throw new TransformerException(
						String.format("Failed to convert the %s value [%s] into a %s for attribute [%s]",
							o.getClass().getCanonicalName(), o, v.getType(), v.getName()),
						e);
				}
			}
			newObject.setAttribute(newAttribute);
		}

		// Create the list of properties to copy...
		for (String s : this.privateProperties.keySet()) {
			DynamicValue v = this.privateProperties.get(s);
			CmfProperty<VALUE> p = new CmfProperty<>(v.getName(), v.getType(), v.isMultivalued());
			if (p.isMultivalued()) {
				for (Object o : v.getValues()) {
					try {
						p.addValue(translator.getValue(v.getType(), o));
					} catch (ParseException e) {
						throw new TransformerException(
							String.format("Failed to convert the %s value [%s] into a %s for property [%s]",
								o.getClass().getCanonicalName(), o, v.getType(), v.getName()),
							e);
					}
				}
			} else {
				Object o = v.getValue();
				try {
					p.setValue(translator.getValue(v.getType(), o));
				} catch (ParseException e) {
					throw new TransformerException(
						String.format("Failed to convert the %s value [%s] into a %s for property [%s]",
							o.getClass().getCanonicalName(), o, v.getType(), v.getName()),
						e);
				}
			}
			newObject.setProperty(p);
		}

		return newObject;
	}
}