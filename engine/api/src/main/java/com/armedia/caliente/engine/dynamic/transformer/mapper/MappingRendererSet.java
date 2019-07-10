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
package com.armedia.caliente.engine.dynamic.transformer.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.xml.mapper.ResidualsMode;
import com.armedia.commons.utilities.Tools;

public class MappingRendererSet
	implements BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>> {

	private final String name;
	private final ResidualsMode residualsMode;
	private final Character separator;
	private final List<BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>>> renderers;

	public MappingRendererSet(String name, Character separator, ResidualsMode residualsMode,
		List<BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>>> renderers) {
		this.name = name;
		this.renderers = Tools.freezeList(renderers);
		this.residualsMode = residualsMode;
		this.separator = separator;
	}

	public String getName() {
		return this.name;
	}

	public List<BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>>> getRenderers() {
		return this.renderers;
	}

	public final ResidualsMode getResidualsMode() {
		return this.residualsMode;
	}

	public Character getSeparator() {
		return this.separator;
	}

	/**
	 * Render the mapped values, and return the attribute values that were rendered.
	 *
	 * @param object
	 * @return the set of target attributes that were rendered
	 */
	@Override
	public final Collection<AttributeMapping> apply(DynamicObject object, ResidualsModeTracker tracker) {
		Map<String, AttributeMapping> ret = new TreeMap<>();
		if (tracker != null) {
			tracker.applyResidualsMode(this.residualsMode);
		}
		for (BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>> r : this.renderers) {
			if (r == null) {
				continue;
			}

			Collection<AttributeMapping> values = r.apply(object, tracker);
			if ((values == null) || values.isEmpty()) {
				continue;
			}

			for (AttributeMapping value : values) {
				final String targetName = value.getTargetName();
				if (value.isOverride() || !ret.containsKey(targetName)) {
					ret.put(targetName, value);
				}
			}
		}
		return ret.values();
	}
}