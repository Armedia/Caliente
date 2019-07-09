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
package com.armedia.caliente.cli.typedumper;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.documentum.fc.client.IDfType;

public class DqlTypePersistor extends BaseShareableLockable implements TypePersistor {

	private PrintWriter out = null;

	@Override
	public void initialize(final File target) throws Exception {
		final File finalTarget = Tools.canonicalize(target);
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out = new PrintWriter(new FileWriter(finalTarget));
			this.out.printf("" + //
				"/**********************************************************/%n" + //
				"/* BEGIN TYPE DUMPS                                       */%n" + //
				"/**********************************************************/%n" //
			);
			this.out.flush();
		}
	}

	@Override
	public void persist(IDfType type) throws Exception {
		if (type == null) { return; }
		try (MutexAutoLock lock = autoMutexLock()) {
			// TODO: Render the DQL For the type
			String extendsClause = "";
			IDfType superType = type.getSuperType();
			if (superType != null) {
				extendsClause = String.format(" (extends %s)", superType.getName());
			}
			this.out.printf("Found type %s%s%n", type.getName(), extendsClause);
		}
	}

	@Override
	public void close() throws Exception {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out.printf("" + //
				"/**********************************************************/%n" + //
				"/* END TYPE DUMPS                                         */%n" + //
				"/**********************************************************/%n" //
			);
			this.out.flush();
			this.out.close();
		}
	}
}