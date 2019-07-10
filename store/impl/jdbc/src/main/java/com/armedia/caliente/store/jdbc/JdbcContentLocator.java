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
package com.armedia.caliente.store.jdbc;

import java.io.Serializable;

import com.armedia.caliente.store.CmfContentStream;
import com.armedia.commons.utilities.Tools;

public class JdbcContentLocator implements Serializable, Comparable<JdbcContentLocator> {
	private static final long serialVersionUID = 1L;

	private final String objectId;
	private final CmfContentStream info;

	JdbcContentLocator(String objectId, CmfContentStream info) {
		if (objectId == null) { throw new IllegalArgumentException("Must provide a non-null object ID"); }
		if (info == null) { throw new IllegalArgumentException("Must provide a non-null content info object"); }
		this.objectId = objectId;
		this.info = info;
	}

	public String getObjectId() {
		return this.objectId;
	}

	public CmfContentStream getInfo() {
		return this.info;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.objectId, this.info);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		JdbcContentLocator other = JdbcContentLocator.class.cast(obj);
		if (!Tools.equals(this.objectId, other.objectId)) { return false; }
		if (!Tools.equals(this.info, other.info)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("JdbcContentLocator [objectId=%s, qualifier=%s]", this.objectId, this.info);
	}

	@Override
	public int compareTo(JdbcContentLocator o) {
		if (o == null) { return 1; }
		if (o == this) { return 0; }
		int r = Tools.compare(this.objectId, o.objectId);
		if (r != 0) { return r; }
		r = Tools.compare(this.info, o.info);
		if (r != 0) { return r; }
		return r;
	}
}