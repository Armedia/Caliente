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
package com.armedia.caliente.engine.cmis;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.commons.data.Acl;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

public final class CmisAcl {

	private final CmfObject.Archetype sourceType;
	private final String sourceId;
	private final String sourceOwner;
	private final Acl acl;

	public CmisAcl(CmfObject.Archetype sourceType, CmisObject object) {
		this.sourceType = sourceType;
		this.sourceId = object.getId();
		this.sourceOwner = object.getCreatedBy();
		this.acl = object.getAcl();
	}

	public CmfObject.Archetype getSourceType() {
		return this.sourceType;
	}

	public String getSourceId() {
		return this.sourceId;
	}

	public String getSourceOwner() {
		return this.sourceOwner;
	}

	public Acl getAcl() {
		return this.acl;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.sourceType, this.sourceId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmisAcl other = CmisAcl.class.cast(obj);
		if (this.sourceType != other.sourceType) { return false; }
		if (!Tools.equals(this.sourceId, other.sourceId)) { return false; }
		return true;
	}
}