package com.armedia.caliente.store;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public enum CmfType {
	//
	DATASTORE, //
	USER, //
	GROUP, //
	ACL, //
	TYPE, //
	FORMAT, //
	FOLDER, //
	DOCUMENT, //
	// RELATION, //
	//
	;

	private static final Set<String> names;
	static {
		Set<String> n = new TreeSet<>();
		for (CmfType t : CmfType.values()) {
			n.add(t.name());
		}
		names = Tools.freezeSet(new LinkedHashSet<>(n));
	}

	public static Set<String> getNames() {
		return CmfType.names;
	}
}