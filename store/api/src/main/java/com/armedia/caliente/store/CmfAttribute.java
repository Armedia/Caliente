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
package com.armedia.caliente.store;

import java.util.Collection;

public class CmfAttribute<VALUE> extends CmfProperty<VALUE> {

	public CmfAttribute(CmfAttribute<VALUE> pattern) {
		super(pattern);
	}

	public CmfAttribute(CmfEncodeableName name, CmfValue.Type type, boolean multivalue) {
		super(name, type, multivalue);
	}

	public CmfAttribute(CmfEncodeableName name, CmfValue.Type type, boolean multivalue, Collection<VALUE> values) {
		super(name, type, multivalue, values);
	}

	public CmfAttribute(String name, CmfValue.Type type, boolean multivalue) {
		super(name, type, multivalue);
	}

	public CmfAttribute(String name, CmfValue.Type type, boolean multivalue, Collection<VALUE> values) {
		super(name, type, multivalue, values);
	}

	@Override
	public CmfAttribute<VALUE> setValues(Collection<VALUE> values) {
		super.setValues(values);
		return this;
	}

	@Override
	public CmfAttribute<VALUE> addValue(VALUE value) {
		super.addValue(value);
		return this;
	}

	@Override
	public CmfAttribute<VALUE> addValues(Collection<VALUE> values) {
		super.addValues(values);
		return this;
	}

	@Override
	public CmfAttribute<VALUE> setValue(VALUE value) {
		super.setValue(value);
		return this;
	}

	@Override
	public String toString() {
		return String.format("CmfAttribute [name=%s, type=%s, repeating=%s %s=%s]", getName(), getType(),
			isMultivalued(), (isMultivalued() ? "values" : "singleValue"),
			(isMultivalued() ? getValues() : getValue()));
	}
}