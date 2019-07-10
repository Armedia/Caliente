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
package com.armedia.caliente.engine.dynamic.transformer.mapper.schema;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public final class AttributeDeclaration {
	public final String name;
	public final CmfValue.Type type;
	public final boolean multiple;
	public final boolean required;

	public AttributeDeclaration(String name, CmfValue.Type type, boolean required, boolean multiple) {
		this.name = name;
		this.type = type;
		this.required = required;
		this.multiple = multiple;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.required, this.multiple);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		AttributeDeclaration other = AttributeDeclaration.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.required != other.required) { return false; }
		if (this.multiple != other.multiple) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("AttributeDeclaration [name=%s, type=%s, required=%s, multiple=%s]", this.name, this.type,
			this.required, this.multiple);
	}
}