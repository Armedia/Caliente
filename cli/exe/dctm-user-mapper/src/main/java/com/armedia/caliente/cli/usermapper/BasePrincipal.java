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
package com.armedia.caliente.cli.usermapper;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasePrincipal implements Serializable {
	private static final long serialVersionUID = 1L;

	protected static final Logger LOG = LoggerFactory.getLogger(BasePrincipal.class);

	private final String name;
	private final String guid;

	/**
	 * @param name
	 * @param guid
	 */
	public BasePrincipal(String name, String guid) {
		this.name = name;
		this.guid = guid;
	}

	public String getName() {
		return this.name;
	}

	public String getGuid() {
		return this.guid;
	}

	@Override
	public String toString() {
		return String.format("%s [name=%s, guid=%s]", getClass().getSimpleName(), getName(), getGuid());
	}
}