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

import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.store.CmfValue;

public enum ExportSetting implements TransferEngineSetting {
	//
	FROM(CmfValue.Type.STRING),
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private ExportSetting(CmfValue.Type type) {
		this(type, null);
	}

	private ExportSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private ExportSetting(CmfValue.Type type, Object defaultValue, boolean required) {
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