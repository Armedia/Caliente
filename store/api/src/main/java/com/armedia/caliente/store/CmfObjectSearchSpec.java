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

import com.armedia.commons.utilities.Tools;

public class CmfObjectSearchSpec extends CmfObjectRef {
	private static final long serialVersionUID = 1L;

	private final String searchKey;

	public CmfObjectSearchSpec(CmfObject.Archetype type, String id) {
		this(type, id, id);
	}

	public CmfObjectSearchSpec(CmfObject.Archetype type, String id, String searchKey) {
		super(type, id);
		this.searchKey = Tools.coalesce(searchKey, id);
	}

	protected CmfObjectSearchSpec(CmfObjectSearchSpec other) {
		super(other);
		this.searchKey = other.searchKey;
	}

	public final String getSearchKey() {
		return this.searchKey;
	}

	@Override
	public boolean isNull() {
		return (this.searchKey == null) || super.isNull();
	}

	@Override
	public String toString() {
		return String.format("CmfObjectSearchSpec [type=%s, id=%s, searchKey=%s]", getType().name(), getId(),
			getSearchKey());
	}
}