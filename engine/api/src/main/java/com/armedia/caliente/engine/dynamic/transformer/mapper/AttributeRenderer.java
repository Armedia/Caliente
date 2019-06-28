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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.xml.mapper.Mapping;
import com.armedia.commons.utilities.Tools;

class AttributeRenderer implements BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>> {

	private static final char DEFAULT_SEPARATOR = ',';

	protected final String target;
	protected final Set<String> sourceValues;
	protected final char separator;
	protected final boolean caseSensitive;
	protected final boolean override;

	protected AttributeRenderer(Mapping mapping, Character parentSeparator) {
		this.target = StringUtils.strip(mapping.getTgt());
		this.caseSensitive = mapping.isCaseSensitive();
		this.separator = Tools.coalesce(mapping.getSeparator(), parentSeparator, AttributeRenderer.DEFAULT_SEPARATOR);
		String value = StringUtils.strip(mapping.getValue());
		Stream<String> sourceValues = Tools.splitEscaped(this.separator, value).stream();
		if (!this.caseSensitive) {
			sourceValues = sourceValues.map(StringUtils::upperCase);
		}
		this.sourceValues = Tools.freezeSet(sourceValues.collect(Collectors.toCollection(LinkedHashSet::new)));
		this.override = mapping.isOverride();
	}

	@Override
	public Collection<AttributeMapping> apply(DynamicObject object, ResidualsModeTracker tracker) {
		if (this.caseSensitive) {
			// If the mapping is case sensitive, we don't need to do a scanning search
			for (String sourceName : this.sourceValues) {
				if (!object.getAtt().containsKey(sourceName)) {
					// No match!! Skip!
					continue;
				}

				// Match!!! Return it!!
				return Collections.singletonList(
					new AttributeMapping(object.getAtt().get(sourceName), this.target, this.separator, this.override));
			}
			return Collections.emptyList();
		}

		// The mapping is case-insensitive, so we actually need to do a scanning search
		for (final String sourceName : object.getAtt().keySet()) {
			String sn = StringUtils.upperCase(sourceName);
			if (!this.sourceValues.contains(sn)) {
				// No match!! Skip!
				continue;
			}

			// Match!!! Return it!!
			return Collections.singletonList(
				new AttributeMapping(object.getAtt().get(sourceName), this.target, this.separator, this.override));
		}

		// No match!! Return an empty list!
		return Collections.emptyList();
	}

}