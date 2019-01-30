package com.armedia.caliente.store;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public enum CmfArchetype {
	//
	DATASTORE("ds"), //
	USER("usr"), //
	GROUP("grp"), //
	ACL, //
	TYPE("typ"), //
	FORMAT("fmt"), //
	FOLDER("fld"), //
	DOCUMENT("doc"), //
	// RELATION("rel"), //
	//
	;

	public final String abbrev;

	private CmfArchetype() {
		this(null);
	}

	private CmfArchetype(String abbreviation) {
		this.abbrev = StringUtils.lowerCase(Tools.coalesce(abbreviation, name()));
	}

	private static final Map<String, CmfArchetype> ABBREV;
	private static final Set<String> NAMES;
	static {
		Map<String, CmfArchetype> abb = new TreeMap<>();
		Set<String> n = new TreeSet<>();
		for (CmfArchetype t : CmfArchetype.values()) {
			n.add(t.name());
			CmfArchetype o = abb.put(t.abbrev, t);
			if (o != null) {
				throw new RuntimeException(
					String.format("ERROR: The CmfType values %s and %s share the same abbreviation [%s]", t.name(),
						o.name(), t.abbrev));
			}
		}
		NAMES = Tools.freezeSet(new LinkedHashSet<>(n));
		ABBREV = Tools.freezeMap(new LinkedHashMap<>(abb));
	}

	public static Set<String> getNames() {
		return CmfArchetype.NAMES;
	}

	public static CmfArchetype decode(String value) {
		if (value == null) { return null; }
		try {
			return CmfArchetype.valueOf(StringUtils.upperCase(value));
		} catch (final IllegalArgumentException e) {
			// Maybe an abbreviation?
			CmfArchetype t = CmfArchetype.ABBREV.get(StringUtils.lowerCase(value));
			if (t != null) { return t; }
			throw e;
		}
	}
}