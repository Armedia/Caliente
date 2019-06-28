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

import java.io.Serializable;

import com.armedia.commons.utilities.Tools;

public class CmfObjectRef implements Comparable<CmfObjectRef>, Serializable {
	private static final long serialVersionUID = 1L;

	private final CmfObject.Archetype type;
	private final String id;

	public CmfObjectRef(CmfObjectRef other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another object to build from"); }
		this.type = other.type;
		this.id = other.id;
	}

	public CmfObjectRef(CmfObject.Archetype type, String id) {
		if (type == null) { throw new IllegalArgumentException("Must provide the object's type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide the object's ID"); }
		this.type = type;
		this.id = id;
	}

	public final CmfObject.Archetype getType() {
		return this.type;
	}

	public final String getId() {
		return this.id;
	}

	public boolean isNull() {
		return (this.type == null) || (this.id == null);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfObjectRef other = CmfObjectRef.class.cast(obj);
		if (this.type != other.type) { return false; }
		if (!Tools.equals(this.id, other.id)) { return false; }
		return true;
	}

	@Override
	public int compareTo(CmfObjectRef o) {
		if (o == null) { return 1; }
		int r = Tools.compare(this.type, o.type);
		if (r != 0) { return r; }
		r = Tools.compare(this.id, o.id);
		if (r != 0) { return r; }
		return 0;
	}

	@Override
	public String toString() {
		return String.format("CmfObjectRef [type=%s, id=%s]", this.type.name(), this.id);
	}

	public final String getShortLabel() {
		return String.format("%s[%s]", this.type.name(), this.id);
	}
}