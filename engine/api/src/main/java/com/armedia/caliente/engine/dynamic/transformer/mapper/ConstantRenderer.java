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
package com.armedia.caliente.engine.dynamic.transformer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.xml.mapper.SetValue;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

class ConstantRenderer extends AttributeRenderer {

	private final Collection<AttributeMapping> value;

	public ConstantRenderer(SetValue m, Character parentSeparator) {
		super(m, parentSeparator);
		List<CmfValue> values = new ArrayList<>();
		final CmfValue.Type dataType = m.getType();
		Tools.splitEscaped(this.separator, m.getValue()).forEach((v) -> {
			try {
				values.add(dataType.getSerializer().deserialize(v));
			} catch (Exception e) {
				throw new RuntimeException(String.format(
					"Failed to deserialize the value [%s] as a %s for the constant mapping named [%s] (separator = [%s], full value = [%s])",
					v, dataType.name(), m.getTgt(), this.separator, m.getValue()));
			}
		});
		this.value = Collections
			.singleton(new AttributeMapping(m.getTgt(), this.separator, m.isOverride(), dataType, values));
	}

	@Override
	public Collection<AttributeMapping> apply(DynamicObject object, ResidualsModeTracker tracker) {
		return this.value;
	}
}