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
package com.armedia.caliente.cli.exception;

import com.armedia.caliente.cli.Option;

public class DuplicateOptionException extends CommandLineRuntimeException {
	private static final long serialVersionUID = 1L;

	private final Option existing;
	private final Option incoming;

	public DuplicateOptionException(String msg, Option existing, Option incoming) {
		super(msg);
		if (existing == null) { throw new IllegalArgumentException("Must have an existing value"); }
		if (incoming == null) { throw new IllegalArgumentException("Must have an incoming value"); }
		if (incoming == existing) {
			throw new IllegalArgumentException("The existing and incoming definitions must be different");
		}
		this.existing = existing;
		this.incoming = incoming;
	}

	public Option getExisting() {
		return this.existing;
	}

	public Option getIncoming() {
		return this.incoming;
	}
}