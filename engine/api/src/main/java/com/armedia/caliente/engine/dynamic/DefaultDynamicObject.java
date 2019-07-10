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
package com.armedia.caliente.engine.dynamic;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

public class DefaultDynamicObject extends DynamicObject {

	private final CmfObject<CmfValue> object;
	private final Set<String> secondaries;

	public DefaultDynamicObject(CmfObject<CmfValue> object) {
		Objects.requireNonNull(object, "Must provide a CmfObject to pattern this instance on");
		this.object = object;
		for (CmfAttribute<CmfValue> att : object.getAttributes()) {
			this.attributes.put(att.getName(), new DynamicValue(att));
		}

		for (CmfProperty<CmfValue> prop : object.getProperties()) {
			this.privateProperties.put(prop.getName(), new DynamicValue(prop));
		}

		this.secondaries = new LinkedHashSet<>(object.getSecondarySubtypes());
	}

	@Override
	public String getObjectId() {
		return this.object.getId();
	}

	@Override
	public String getHistoryId() {
		return this.object.getHistoryId();
	}

	@Override
	public boolean isHistoryCurrent() {
		return this.object.isHistoryCurrent();
	}

	@Override
	public CmfObject.Archetype getType() {
		return this.object.getType();
	}

	@Override
	public String getLabel() {
		return this.object.getLabel();
	}

	@Override
	public String getOriginalSubtype() {
		return this.object.getSubtype();
	}

	@Override
	public Set<String> getOriginalSecondarySubtypes() {
		return this.object.getSecondarySubtypes();
	}

	@Override
	public Set<String> getSecondarySubtypes() {
		return this.secondaries;
	}

	@Override
	public int getDependencyTier() {
		return this.object.getDependencyTier();
	}

	@Override
	public String getOriginalName() {
		return this.object.getName();
	}

}