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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.ConstructedType;
import com.armedia.commons.utilities.Tools;

public class AttributeMappingResult implements Iterable<AttributeMapping> {

	private final ConstructedType type;
	private final Map<String, AttributeMapping> mappings;
	private final boolean residualsEnabled;

	AttributeMappingResult(ConstructedType type, Map<String, AttributeMapping> mappings, boolean residualsEnabled) {
		this.type = type;
		this.mappings = Tools.freezeMap(new LinkedHashMap<>(mappings));
		this.residualsEnabled = residualsEnabled;
	}

	public final ConstructedType getType() {
		return this.type;
	}

	public Set<String> getAttributeNames() {
		return this.mappings.keySet();
	}

	public AttributeMapping getAttributeMapping(String name) {
		if (name == null) { return null; }
		return this.mappings.get(name);
	}

	public boolean hasAttributeValue(String name) {
		if (name == null) { return false; }
		return this.mappings.containsKey(name);
	}

	public boolean isResidualsEnabled() {
		return this.residualsEnabled;
	}

	@Override
	public Iterator<AttributeMapping> iterator() {
		return this.mappings.values().iterator();
	}
}