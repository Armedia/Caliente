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
package com.armedia.caliente.engine;

import com.armedia.caliente.store.CmfValue;

public enum TransferSetting implements TransferEngineSetting {
	//
	EXCEPT_TYPES(CmfValue.Type.STRING),
	ONLY_TYPES(CmfValue.Type.STRING),
	IGNORE_CONTENT(CmfValue.Type.BOOLEAN, false),
	THREAD_COUNT(CmfValue.Type.INTEGER),
	BACKLOG_SIZE(CmfValue.Type.INTEGER),
	LATEST_ONLY(CmfValue.Type.BOOLEAN, false),
	NO_RENDITIONS(CmfValue.Type.BOOLEAN, false),
	RETRY_ATTEMPTS(CmfValue.Type.INTEGER, 2),
	TRANSFORMATION(CmfValue.Type.STRING),
	FILTER(CmfValue.Type.STRING),
	EXTERNAL_METADATA(CmfValue.Type.STRING),
	USER_MAP(CmfValue.Type.STRING) {
		@Override
		public Object getDefaultValue() {
			return PrincipalType.USER.getDefaultMappingFile();
		}
	},
	GROUP_MAP(CmfValue.Type.STRING) {
		@Override
		public Object getDefaultValue() {
			return PrincipalType.GROUP.getDefaultMappingFile();
		}
	},
	//
	;

	private final String label;
	private final Object defaultValue;
	private final CmfValue.Type type;
	private final boolean required;

	private TransferSetting(CmfValue.Type type) {
		this(type, null);
	}

	private TransferSetting(CmfValue.Type type, Object defaultValue) {
		this(type, defaultValue, false);
	}

	private TransferSetting(CmfValue.Type type, Object defaultValue, boolean required) {
		if (type == null) { throw new IllegalArgumentException("Must provide a data type"); }
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
	public Object getDefaultValue() {
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