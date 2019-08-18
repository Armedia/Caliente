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
package com.armedia.caliente.cli.typedumper;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.IDfAttr;

public class DqlTypePersistor extends BaseShareableLockable implements TypePersistor {

	private static final String NL = String.format("%n");
	private static final String HEADER = "BEGIN TYPE DUMPS";
	private static final String FOOTER = "END TYPE DUMPS";
	private static final String BIG_DIVIDER = StringUtils.repeat('-', 80);
	private static final String SMALL_DIVIDER = StringUtils.repeat('-', 60);

	private static final String pad(String message, String divider, int extra) {
		int padLength = (divider.length() - message.length() - Math.max(0, extra));
		return message + StringUtils.repeat(' ', Math.max(0, padLength));
	}

	private PrintWriter out = null;

	@Override
	public void initialize(final File target) throws Exception {
		final File finalTarget = Tools.canonicalize(target);
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out = new PrintWriter(new FileWriter(finalTarget));
			this.out.printf("" + //
				DqlTypePersistor.BIG_DIVIDER + "%n" + //
				"-- " + DqlTypePersistor.pad(DqlTypePersistor.HEADER, DqlTypePersistor.BIG_DIVIDER, 6) + " --%n" + //
				DqlTypePersistor.BIG_DIVIDER + "%n" //
			);
			this.out.flush();
		}
	}

	@Override
	public void persist(IDfType type) throws Exception {
		if (type == null) { return; }
		try (MutexAutoLock lock = autoMutexLock()) {
			String extendsClause = "";
			final IDfType superType = type.getSuperType();

			final StringBuilder dql = new StringBuilder();
			final int attrCount = type.getInt("attr_count");
			final int startPosition = type.getInt("start_pos");
			final boolean parens = (startPosition < attrCount);

			String typeDecl = String.format("BEGIN TYPE: %s", type.getName());
			if (superType != null) {
				String msg = String.format("   EXTENDS: %s", superType.getName());
				extendsClause = "-- " + DqlTypePersistor.pad(msg, DqlTypePersistor.SMALL_DIVIDER, 6) + " --%n";
			}
			this.out.printf("%n" + DqlTypePersistor.SMALL_DIVIDER + "%n" + //
				"-- " + DqlTypePersistor.pad(typeDecl, DqlTypePersistor.SMALL_DIVIDER, 6) + " --%n" + //
				extendsClause + //
				DqlTypePersistor.SMALL_DIVIDER + "%n" //
			);

			dql.append("create type \"").append(type.getName()).append("\"");
			if (parens) {
				dql.append("(").append(DqlTypePersistor.NL);
			}
			for (int i = startPosition; i < attrCount; i++) {
				// If we're not the first, we need a comma
				final String attrName = type.getRepeatingString("attr_name", i);
				final int attrType = type.getRepeatingInt("attr_type", i);
				final boolean attrRepeating = type.getRepeatingBoolean("attr_repeating", i);
				final int attrQualified = type.getRepeatingInt("attr_restriction", i);
				if (i > startPosition) {
					dql.append(",").append(DqlTypePersistor.NL);
				}
				dql.append("\t").append(attrName).append("\t");
				switch (attrType) {
					case IDfAttr.DM_BOOLEAN:
						dql.append("boolean");
						break;
					case IDfAttr.DM_INTEGER:
						dql.append("integer");
						break;
					case IDfAttr.DM_STRING:
						final int attrLength = type.getRepeatingInt("attr_length", i);
						dql.append("string(").append(attrLength).append(")");
						break;
					case IDfAttr.DM_ID:
						dql.append("id");
						break;
					case IDfAttr.DM_TIME:
						dql.append("date");
						break;
					case IDfAttr.DM_DOUBLE:
						dql.append("double");
						break;
					case IDfAttr.DM_UNDEFINED:
						dql.append("undefined");
						break;
					default:
						break;
				}
				if (attrRepeating) {
					dql.append(" repeating");
				}
				if (attrQualified != 0) {
					dql.append(" not qualified");
				}
			}
			if (parens) {
				dql.append(DqlTypePersistor.NL).append(")");
			}

			dql.append(" with supertype ").append((superType != null) ? superType.getName() : "null").append(" publish")
				.append(DqlTypePersistor.NL).append("go");

			this.out.printf("%s%n", dql);

			this.out.printf("%n" + DqlTypePersistor.SMALL_DIVIDER + "%n" + //
				"-- " + DqlTypePersistor.pad("END TYPE", DqlTypePersistor.SMALL_DIVIDER, 6) + " --%n" + //
				DqlTypePersistor.SMALL_DIVIDER + "%n" //
			);
		}
	}

	@Override
	public void close() throws Exception {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out.printf("" + //
				DqlTypePersistor.BIG_DIVIDER + "%n" + //
				"-- " + DqlTypePersistor.pad(DqlTypePersistor.FOOTER, DqlTypePersistor.BIG_DIVIDER, 6) + " --%n" + //
				DqlTypePersistor.BIG_DIVIDER + "%n" //
			);
			this.out.flush();
			this.out.close();
		}
	}
}