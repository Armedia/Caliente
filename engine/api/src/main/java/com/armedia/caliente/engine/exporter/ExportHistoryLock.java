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

import java.io.Serializable;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

final class ExportHistoryLock implements Serializable {
	private static final long serialVersionUID = 1L;

	private final CmfObject.Archetype type;
	private final String historyId;
	private final String lockId;

	ExportHistoryLock(CmfObject.Archetype type, String historyId, ExportContext<?, ?, ?> ctx) {
		if (type == null) { throw new IllegalArgumentException("Must provide a non-null object type"); }
		if (historyId == null) { throw new IllegalArgumentException("Must provide a non-null history ID"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a non-null export context"); }
		this.type = type;
		this.historyId = historyId;
		this.lockId = ctx.getId();
	}

	public CmfObject.Archetype getType() {
		return this.type;
	}

	public String getHistoryId() {
		return this.historyId;
	}

	public String getLockId() {
		return this.lockId;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.historyId, this.lockId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ExportHistoryLock other = ExportHistoryLock.class.cast(obj);
		if (!Tools.equals(this.type, other.type)) { return false; }
		if (!Tools.equals(this.historyId, other.historyId)) { return false; }
		if (!Tools.equals(this.lockId, other.lockId)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("ExportHistoryLock [type=%s, historyId=%s, lockId=%s]", this.type.name(), this.historyId,
			this.lockId);
	}
}