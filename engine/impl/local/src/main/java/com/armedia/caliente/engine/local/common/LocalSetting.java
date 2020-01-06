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
package com.armedia.caliente.engine.local.common;

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;

public enum LocalSetting implements TransferEngineSetting {
	//
	ROOT(CmfValue.Type.STRING),
	COPY_CONTENT(CmfValue.Type.BOOLEAN, true),
	INCLUDE_ALL_VERSIONS(CmfValue.Type.BOOLEAN, false),
	INCLUDE_ALL_STREAMS(CmfValue.Type.BOOLEAN, false),
	INCLUDE_METADATA(CmfValue.Type.BOOLEAN, false),
	IGNORE_EMPTY_FOLDERS(CmfValue.Type.BOOLEAN, false),
	FAIL_ON_COLLISIONS(CmfValue.Type.BOOLEAN, true),
	VERSION_SCHEME(CmfValue.Type.STRING, false),
	VERSION_SCHEME_EMPTY_IS_ROOT(CmfValue.Type.BOOLEAN, false),
	VERSION_LAYOUT(CmfValue.Type.STRING, false),
	VERSION_LAYOUT_STREAM_NAME(CmfValue.Type.STRING, false),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private LocalSetting(CmfValue.Type type) {
		this(type, null);
	}

	private LocalSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private LocalSetting(CmfValue.Type type, Object defaultValue, boolean required) {
		this.label = name().toLowerCase();
		this.defaultValue = defaultValue;
		this.type = type;
		this.required = required;
	}

	@Override
	public final String getLabel() {
		return this.label;
	}

	@Override
	public final Object getDefaultValue() {
		return this.defaultValue;
	}

	@Override
	public CmfValue.Type getType() {
		return this.type;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}
}