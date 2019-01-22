package com.armedia.caliente.store;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public enum CmfType {
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

	private CmfType() {
		this(null);
	}

	private CmfType(String abbreviation) {
		this.abbrev = StringUtils.lowerCase(Tools.coalesce(abbreviation, name()));
	}

	private static final Map<String, CmfType> ABBREV;
	private static final Set<String> NAMES;
	static {
		Map<String, CmfType> abb = new TreeMap<>();
		Set<String> n = new TreeSet<>();
		for (CmfType t : CmfType.values()) {
			n.add(t.name());
			CmfType o = abb.put(t.abbrev, t);
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
		return CmfType.NAMES;
	}

	public static CmfType decode(String value) {
		if (value == null) { return null; }
		try {
			return CmfType.valueOf(StringUtils.upperCase(value));
		} catch (final IllegalArgumentException e) {
			// Maybe an abbreviation?
			CmfType t = CmfType.ABBREV.get(StringUtils.lowerCase(value));
			if (t != null) { return t; }
			throw e;
		}
	}
}