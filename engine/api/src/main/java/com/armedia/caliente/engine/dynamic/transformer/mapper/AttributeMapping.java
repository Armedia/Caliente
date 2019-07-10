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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class AttributeMapping implements Iterable<Object> {
	public static final char DEFAULT_SEPARATOR = ',';

	private final String sourceName;
	private final String targetName;
	private final Collection<Object> values;
	private final boolean override;
	private final char separator;
	private final CmfValue.Type type;
	private final boolean repeating;

	AttributeMapping(DynamicValue sourceAttribute, String targetName, char separator, boolean override) {
		this.sourceName = sourceAttribute.getName();
		this.targetName = targetName;
		this.values = sourceAttribute.getValues();
		this.override = override;
		this.separator = separator;
		this.type = sourceAttribute.getType();
		this.repeating = sourceAttribute.isMultivalued();
	}

	AttributeMapping(String targetName, char separator, boolean override, CmfValue.Type type,
		Collection<Object> values) {
		this.sourceName = null;
		this.targetName = targetName;
		this.values = Tools.coalesce(values, Collections.emptyList());
		this.override = override;
		this.separator = separator;
		this.repeating = (values.size() > 1);
		this.type = type;
	}

	AttributeMapping(String targetName, char separator, boolean override, CmfValue.Type type, Object... values) {
		this(targetName, separator, override, type, Arrays.asList(values));
	}

	public CmfValue.Type getType() {
		return this.type;
	}

	public boolean isRepeating() {
		return this.repeating;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getTargetName() {
		return this.targetName;
	}

	public Collection<Object> getValues() {
		return this.values;
	}

	public char getSeparator() {
		return this.separator;
	}

	public boolean isOverride() {
		return this.override;
	}

	public int getValueCount() {
		return this.values.size();
	}

	@Override
	public Iterator<Object> iterator() {
		return this.values.iterator();
	}

	@Override
	public String toString() {
		return Tools.toString(this.values);
	}
}