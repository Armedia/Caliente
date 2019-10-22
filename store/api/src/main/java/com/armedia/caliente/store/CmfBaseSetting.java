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
package com.armedia.caliente.store;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class CmfBaseSetting implements CmfSetting, Comparable<CmfBaseSetting> {

	protected final String name;
	protected final CmfValue.Type type;
	protected final boolean multivalue;

	protected CmfBaseSetting(CmfBaseSetting pattern) {
		if (pattern == null) {
			throw new IllegalArgumentException("Must provide a non-null pattern to copy values from");
		}
		this.name = pattern.name;
		this.type = pattern.type;
		this.multivalue = pattern.multivalue;
	}

	public CmfBaseSetting(String name, CmfValue.Type type) {
		this(name, type, false);
	}

	public CmfBaseSetting(String name, CmfValue.Type type, boolean multivalue) {
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("Must provide a non-null, non-blank name");
		}
		if (type == null) { throw new IllegalArgumentException("Must provide a data type"); }
		this.type = type;
		this.name = name;
		this.multivalue = multivalue;
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final CmfValue.Type getType() {
		return this.type;
	}

	@Override
	public final boolean isMultivalued() {
		return this.multivalue;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.multivalue);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfBaseSetting other = CmfBaseSetting.class.cast(obj);
		if (!Objects.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.multivalue != other.multivalue) { return false; }
		return true;
	}

	@Override
	public int compareTo(CmfBaseSetting o) {
		if (o == null) { return 1; }
		int r = this.name.compareTo(o.name);
		if (r != 0) { return r; }
		r = this.type.compareTo(o.type);
		if (r != 0) { return r; }
		r = Tools.compare(this.multivalue, o.multivalue);
		if (r != 0) { return r; }
		return 0;
	}
}