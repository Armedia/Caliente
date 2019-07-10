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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.xml.mapper.NamespaceMapping;

class NamespaceRenderer extends AttributeRenderer {
	private static final Pattern NSPARSER = Pattern.compile("^([^:]+):(.+)$");

	public NamespaceRenderer(NamespaceMapping m, Character parentSeparator) {
		super(m, parentSeparator);
	}

	@Override
	public Collection<AttributeMapping> apply(DynamicObject object, ResidualsModeTracker tracker) {
		Objects.requireNonNull(object, "Must provide a source object to map against");

		Collection<AttributeMapping> ret = new ArrayList<>();
		object.getAtt().forEach((sourceName, attribute) -> {
			// First, get the source attribute's namespace
			Matcher m = NamespaceRenderer.NSPARSER.matcher(sourceName);
			if (!m.matches()) {
				// No namespace!! Not applicable
				return;
			}

			// We have a namespace...so should it be mapped?
			String srcNs = m.group(1);
			if (!this.caseSensitive) {
				srcNs = StringUtils.upperCase(srcNs);
			}

			if (!this.sourceValues.contains(srcNs)) {
				// No match...skip it!
				return;
			}

			final String attributeBaseName = m.group(2);
			final String targetName = String.format("%s:%s", this.target, attributeBaseName);
			ret.add(new AttributeMapping(attribute, targetName, this.separator, this.override));
		});
		return ret;
	}
}