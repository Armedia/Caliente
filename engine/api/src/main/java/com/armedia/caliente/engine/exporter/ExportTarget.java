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
package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectSearchSpec;

public final class ExportTarget extends CmfObjectSearchSpec {
	private static final long serialVersionUID = 1L;

	private final Long number;

	public ExportTarget(CmfObjectSearchSpec spec) {
		super(spec);
		this.number = null;
	}

	public ExportTarget(CmfObject.Archetype type, String id, String searchKey) {
		this(type, id, searchKey, null);
	}

	public ExportTarget(CmfObject.Archetype type, String id, String searchKey, Long number) {
		super(type, id, searchKey);
		this.number = number;
	}

	public Long getNumber() {
		return this.number;
	}

	@Override
	public String toString() {
		if (this.number != null) {
			return String.format("ExportTarget [type=%s, id=%s, searchKey=%s, number=%s]", getType().name(), getId(),
				getSearchKey(), this.number);
		}
		return String.format("ExportTarget [type=%s, id=%s, searchKey=%s]", getType().name(), getId(), getSearchKey());
	}
}