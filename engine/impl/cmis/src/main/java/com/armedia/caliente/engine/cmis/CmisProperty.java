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
package com.armedia.caliente.engine.cmis;

import java.util.Map;
import java.util.function.Supplier;

import com.armedia.caliente.engine.converter.MappingManager;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public enum CmisProperty implements Supplier<String>, CmfEncodeableName {

	//
	PRODUCT_NAME(CmfValue.Type.STRING),
	PRODUCT_VERSION(CmfValue.Type.STRING),
	ACL_PERMISSION(CmfValue.Type.STRING, true),
	//
	;

	public static final String PERMISSION_PROPERTY_FMT = "cmf:%s:aclPermission";

	public final String name;
	public final CmfValue.Type type;
	public final boolean repeating;

	private CmisProperty(CmfValue.Type type) {
		this(type, false);
	}

	private CmisProperty(CmfValue.Type type, boolean repeating) {
		this.name = String.format("cmis:%s", name().toLowerCase());
		this.type = type;
		this.repeating = repeating;
	}

	@Override
	public final String encode() {
		return this.name;
	}

	@Override
	public String get() {
		return this.name;
	}

	private static final Map<String, CmisProperty> MAPPINGS;

	static {
		MAPPINGS = Tools.freezeMap(MappingManager.createMappings(CmisProperty.class, CmisProperty.values()));
	}

	public static CmisProperty decode(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name to decode"); }
		CmisProperty ret = CmisProperty.MAPPINGS.get(name);
		if (ret == null) {
			throw new IllegalArgumentException(
				String.format("Failed to decode [%s] into a valid intermediate property", name));
		}
		return ret;
	}
}