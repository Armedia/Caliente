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
package com.armedia.caliente.cli.caliente.command;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public enum CalienteCommand {

	//
	EXPORT(
		"Extract content from a source system into a Caliente data store", //
		new String[] {
			"exp", "ex"
		},
		true, //
		true //
	), //
	IMPORT(
		"Ingest content from a Caliente data store into a target system or structure", //
		new String[] {
			"imp", "im"
		},
		true, //
		false //
	), //
	COUNT(
		"Perform an object count analysis within a source system", //
		new String[] {
			"cnt", "cn"
		},
		false, //
		false //
	), //
	ENCRYPT(
		"Encrypt plaintext password values for future Caliente consumption", //
		new String[] {
			"enc"
		},
		false, //
		false //
	), //
	DECRYPT(
		"Decrypt Caliente-encrypted password values", //
		new String[] {
			"dec"
		},
		false, //
		false //
	), //
		//
	;

	private final String title;
	private final String description;
	private final Set<String> aliases;
	private final boolean requiresStorage;
	private final boolean requiresCleanData;

	private CalienteCommand(String description, String[] aliases, boolean requiresStorage, boolean requiresCleanData) {
		this.description = description;
		Set<String> a = new TreeSet<>();
		Arrays.stream(aliases).map(CalienteCommand::canonicalize).filter(StringUtils::isNotBlank).forEach(a::add);
		this.title = CalienteCommand.canonicalize(name());
		this.aliases = Tools.freezeSet(new LinkedHashSet<>(a));
		this.requiresStorage = requiresStorage;
		this.requiresCleanData = requiresCleanData;
	}

	public String getTitle() {
		return this.title;
	}

	public String getDescription() {
		return this.description;
	}

	public Set<String> getAliases() {
		return this.aliases;
	}

	public boolean isRequiresStorage() {
		return this.requiresStorage;
	}

	public boolean isRequiresCleanData() {
		return this.requiresCleanData;
	}

	private static String canonicalize(String s) {
		s = StringUtils.strip(s);
		s = StringUtils.lowerCase(s);
		return s;
	}

	private static final Map<String, CalienteCommand> MAP;

	static {
		Map<String, CalienteCommand> m = new TreeMap<>();
		for (CalienteCommand c : CalienteCommand.values()) {
			CalienteCommand o = m.put(c.title, c);
			if (o != null) {
				// KABOOM!
				throw new IllegalStateException(String.format(
					"CODE ERROR: Commands %s and %s both share the canonical title '%s'", c.name(), o.name(), c.title));
			}

			for (String a : c.aliases) {
				o = m.put(a, c);
				if (o != null) {
					// KABOOM!
					throw new IllegalStateException(String
						.format("CODE ERROR: Commands %s and %s both share the alias '%s'", c.name(), o.name(), a));
				}
			}
		}
		MAP = Tools.freezeMap(new LinkedHashMap<>(m));
	}

	public static CalienteCommand get(String s) {
		s = CalienteCommand.canonicalize(s);
		if (s == null) { return null; }
		return CalienteCommand.MAP.get(s);
	}

	public static Set<String> getAllAliases() {
		return CalienteCommand.MAP.keySet();
	}
}