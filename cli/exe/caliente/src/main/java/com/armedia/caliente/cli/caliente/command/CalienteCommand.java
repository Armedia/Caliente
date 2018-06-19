package com.armedia.caliente.cli.caliente.command;

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

	public final String title;
	public final String description;
	public final Set<String> aliases;
	public final boolean requiresStorage;
	public final boolean requiresCleanData;

	private CalienteCommand(String description, String[] aliases, boolean requiresStorage, boolean requiresCleanData) {
		this.description = description;
		Set<String> a = new TreeSet<>();
		for (String s : aliases) {
			s = CalienteCommand.canonicalize(s);
			if (!StringUtils.isBlank(s)) {
				a.add(s);
			}
		}
		this.title = CalienteCommand.canonicalize(name());
		this.aliases = Tools.freezeSet(new LinkedHashSet<>(a));
		this.requiresStorage = requiresStorage;
		this.requiresCleanData = requiresCleanData;
	}

	private static String canonicalize(String s) {
		s = StringUtils.strip(s);
		s = StringUtils.lowerCase(s);
		return s;
	}

	public static CalienteCommand get(String s) {
		s = CalienteCommand.canonicalize(s);
		if (s == null) { return null; }
		return CalienteCommand.MAP.get(s);
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
}