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
package com.armedia.caliente.engine.exporter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectSearchSpec;

public final class ExportTarget extends CmfObjectSearchSpec {
	private static final long serialVersionUID = 1L;

	private ExportTarget(CmfObjectSearchSpec spec) {
		super(spec);
	}

	private ExportTarget(CmfObject.Archetype type, String id, String searchKey) {
		super(type, id, searchKey);
	}

	@Override
	public String toString() {
		return String.format("ExportTarget [type=%s, id=%s, searchKey=%s]", getType().name(), getId(), getSearchKey());
	}

	public static ExportTarget from(CmfObjectSearchSpec spec) {
		if (spec == null) { return null; }
		return new ExportTarget(spec);
	}

	public static ExportTarget from(CmfObjectRef ref) {
		if (ref == null) { return null; }
		return new ExportTarget(ref.getType(), ref.getId(), null);
	}

	public static ExportTarget from(CmfObject.Archetype type, String id, String searchKey) {
		if (type == null) { return null; }
		if (StringUtils.isEmpty(id)) { return null; }
		return new ExportTarget(type, id, searchKey);
	}
}