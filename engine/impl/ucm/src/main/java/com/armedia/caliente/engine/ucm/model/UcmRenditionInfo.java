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
package com.armedia.caliente.engine.ucm.model;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet.Field;

public final class UcmRenditionInfo {

	public static final String DEFAULT = "primary";

	private final UcmUniqueURI guid;
	private final String type;
	private final String format;
	private final String name;
	private final String description;

	UcmRenditionInfo(UcmUniqueURI guid, DataObject obj, Collection<Field> structure) {
		UcmAttributes data = new UcmAttributes(obj, structure);
		this.guid = guid;
		this.type = data.getString(UcmAtt.rendType);
		this.format = data.getString(UcmAtt.rendFormat);
		this.name = data.getString(UcmAtt.rendName);
		this.description = data.getString(UcmAtt.rendDescription);
	}

	UcmRenditionInfo(UcmUniqueURI guid, String type, String format, String name, String description) {
		this.guid = guid;
		this.type = type;
		this.format = format;
		this.name = name;
		this.description = description;
	}

	public UcmUniqueURI getGuid() {
		return this.guid;
	}

	public String getType() {
		return this.type;
	}

	public String getFormat() {
		return this.format;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public boolean isPrimary() {
		return StringUtils.equalsIgnoreCase(this.name, UcmRenditionInfo.DEFAULT);
	}

	public boolean isDefault() {
		return isPrimary();
	}
}