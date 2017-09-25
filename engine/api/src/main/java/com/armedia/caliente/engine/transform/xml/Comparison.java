package com.armedia.caliente.engine.transform.xml;

import com.armedia.commons.utilities.Tools;

public enum Comparison {
	//
	EQ() {
		@Override
		protected boolean eval(String source, String target) {
			return Tools.equals(source, target);
		}
	},
	NE() {
		@Override
		protected boolean eval(String source, String target) {
			return !EQ.eval(source, target);
		}
	},
	GT() {
		@Override
		protected boolean eval(String source, String target) {
			return Tools.compare(source, target) > 0;
		}
	},
	GE() {
		@Override
		protected boolean eval(String source, String target) {
			return Tools.compare(source, target) >= 0;
		}
	},
	LT() {
		@Override
		protected boolean eval(String source, String target) {
			return !GE.eval(source, target);
		}
	},
	LE() {
		@Override
		protected boolean eval(String source, String target) {
			return !GT.eval(source, target);
		}
	},
	SW() {
		@Override
		protected boolean eval(String source, String target) {
			return source.startsWith(target);
		}
	},
	EW() {
		@Override
		protected boolean eval(String source, String target) {
			return source.endsWith(target);
		}
	},
	CN() {
		@Override
		protected boolean eval(String source, String target) {
			return (source.indexOf(target) >= 0);
		}
	},
	NC() {
		@Override
		protected boolean eval(String source, String target) {
			return !CN.eval(source, target);
		}
	},
	RE() {
		@Override
		protected boolean eval(String source, String target) {
			return target.matches(source);
		}
	},
	NRE() {
		@Override
		protected boolean eval(String source, String target) {
			return !RE.eval(source, target);
		}
	},
	GLOB() {
		@Override
		protected boolean eval(String source, String target) {
			return RE.eval(Tools.globToRegex(source), target);
		}
	},
	NGLOB() {
		@Override
		protected boolean eval(String source, String target) {
			return !GLOB.eval(source, target);
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

	protected boolean eval(String source, String target) {
		// The default implementation only looks for the case-insensitive counterpart.
		// This way we only have to provide the comparison implementation assuming case
		// sensitivity.
		String name = name().toLowerCase();
		if (!name.endsWith(
			"i")) { throw new AbstractMethodError("Must provide a concrete implementation for the comparison check"); }

		// Case-insensitive, find my counterpart!
		Comparison comp = Comparison.valueOf(name.substring(0, name.length() - 1));
		source = source.toUpperCase();
		target = target.toUpperCase();
		return comp.eval(source, target);
	}

	public final boolean evaluate(String source, String target) {
		source = Tools.coalesce(source, "");
		target = Tools.coalesce(target, "");

		return eval(source, target);
	}

	public static Comparison get(String value) {
		return Comparison.get(value, null);
	}

	public static Comparison get(String value, Comparison def) {
		if (value == null) { return def; }
		return Comparison.valueOf(value.toUpperCase());
	}
}