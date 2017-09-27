package com.armedia.caliente.engine.transform;

import java.util.Objects;

import com.armedia.commons.utilities.Tools;

public enum Comparison {
	//
	EQ() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return Tools.equals(candidate, comparand);
		}
	},
	NE() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return !EQ.eval(candidate, comparand);
		}
	},
	GT() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return Tools.compare(candidate, comparand) > 0;
		}
	},
	GE() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return Tools.compare(candidate, comparand) >= 0;
		}
	},
	LT() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return !GE.eval(candidate, comparand);
		}
	},
	LE() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return !GT.eval(candidate, comparand);
		}
	},
	SW() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return candidate.startsWith(comparand);
		}
	},
	EW() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return candidate.endsWith(comparand);
		}
	},
	CN() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return (candidate.indexOf(comparand) >= 0);
		}
	},
	NC() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return !CN.eval(candidate, comparand);
		}
	},
	RE() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return comparand.matches(candidate);
		}
	},
	NRE() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return !RE.eval(candidate, comparand);
		}
	},
	GLOB() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return RE.eval(candidate, Tools.globToRegex(comparand));
		}
	},
	NGLOB() {
		@Override
		protected boolean eval(String candidate, String comparand) {
			return !GLOB.eval(candidate, comparand);
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

	public static Comparison DEFAULT = Comparison.EQI;

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

	protected boolean eval(String candidate, String comparand) {
		// The default implementation only looks for the case-insensitive counterpart.
		// This way we only have to provide the comparison implementation assuming case
		// sensitivity.
		String name = name().toLowerCase();
		if (!name.endsWith("i")) { throw new AbstractMethodError(
			String.format("Must provide a concrete implementation for the %s comparison check", name())); }

		// Case-insensitive, find my counterpart!
		Comparison comp = Comparison.valueOf(name.substring(0, name.length() - 1));
		comparand = comparand.toUpperCase();
		candidate = (candidate != null ? candidate.toUpperCase() : null);
		return comp.eval(candidate, comparand);
	}

	public final boolean check(String candidate, String comparand) {
		Objects.requireNonNull(comparand, "Must provide a non-null comparand value to check the candidate against");
		return eval(candidate, comparand);
	}

	public static Comparison get(String value) {
		return Comparison.get(value, null);
	}

	public static Comparison get(String value, Comparison def) {
		if (value == null) { return def; }
		return Comparison.valueOf(value.toUpperCase());
	}
}