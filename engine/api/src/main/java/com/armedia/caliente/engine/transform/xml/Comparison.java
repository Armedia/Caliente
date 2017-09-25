package com.armedia.caliente.engine.transform.xml;

import com.armedia.commons.utilities.Tools;

public enum Comparison {
	//
	EQ() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return Tools.equals(comparand, candidate);
		}
	},
	NE() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return !EQ.eval(comparand, candidate);
		}
	},
	GT() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return Tools.compare(comparand, candidate) > 0;
		}
	},
	GE() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return Tools.compare(comparand, candidate) >= 0;
		}
	},
	LT() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return !GE.eval(comparand, candidate);
		}
	},
	LE() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return !GT.eval(comparand, candidate);
		}
	},
	SW() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return comparand.startsWith(candidate);
		}
	},
	EW() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return comparand.endsWith(candidate);
		}
	},
	CN() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return (comparand.indexOf(candidate) >= 0);
		}
	},
	NC() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return !CN.eval(comparand, candidate);
		}
	},
	RE() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return candidate.matches(comparand);
		}
	},
	NRE() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return !RE.eval(comparand, candidate);
		}
	},
	GLOB() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return RE.eval(Tools.globToRegex(comparand), candidate);
		}
	},
	NGLOB() {
		@Override
		protected boolean eval(String comparand, String candidate) {
			return !GLOB.eval(comparand, candidate);
		}
	},

	// Add the enum values for case-insensitive checks
	EQI(), //
	NEI(), //
	GTI(), //
	GEI(), //
	LTI(), //
	LEI(), //
	SWI(), //
	EWI(), //
	CNI(), //
	NCI(), //
	REI(), //
	NREI(), //
	GLOBI(), //
	NGLOBI(), //
	//
	;

	static {
		for (Comparison c : Comparison.values()) {
			String name = c.name();
			if (name.endsWith("I")) {
				Comparison.valueOf(name.substring(0, name.length() - 1));
			} else {
				Comparison.valueOf(String.format("%sI", name));
			}
		}
	}

	protected boolean eval(String comparand, String candidate) {
		// The default implementation only looks for the case-insensitive counterpart.
		// This way we only have to provide the comparison implementation assuming case
		// sensitivity.
		String name = name().toLowerCase();
		if (!name.endsWith(
			"i")) { throw new AbstractMethodError("Must provide a concrete implementation for the comparison check"); }

		// Case-insensitive, find my counterpart!
		Comparison comp = Comparison.valueOf(name.substring(0, name.length() - 1));
		comparand = comparand.toUpperCase();
		candidate = candidate.toUpperCase();
		return comp.eval(comparand, candidate);
	}

	public final boolean evaluate(String comparand, String candidate) {
		comparand = Tools.coalesce(comparand, "");
		candidate = Tools.coalesce(candidate, "");

		return eval(comparand, candidate);
	}

	public static Comparison get(String value) {
		return Comparison.get(value, null);
	}

	public static Comparison get(String value, Comparison def) {
		if (value == null) { return def; }
		return Comparison.valueOf(value.toUpperCase());
	}
}