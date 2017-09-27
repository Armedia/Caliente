package com.armedia.caliente.engine.transform.xml;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.armedia.caliente.engine.transform.RuntimeTransformationException;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public enum Comparison {
	//
	EQ() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return Tools.equals(candidate, comparand);
		}
	},
	NE() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return !EQ.eval(type, candidate, comparand);
		}
	},
	GT() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return (Comparison.compare(type, candidate, comparand) > 0);
		}
	},
	GE() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return (Comparison.compare(type, candidate, comparand) >= 0);
		}
	},
	LT() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return !GE.eval(type, candidate, comparand);
		}
	},
	LE() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return !GT.eval(type, candidate, comparand);
		}
	},
	SW() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (comparand == null)) { return false; }
			return candidate.toString().startsWith(comparand.toString());
		}
	},
	EW() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (comparand == null)) { return false; }
			return candidate.toString().endsWith(comparand.toString());
		}
	},
	CN() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (comparand == null)) { return false; }
			return (candidate.toString().indexOf(comparand.toString()) >= 0);
		}
	},
	NC() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return !CN.eval(CmfDataType.STRING, candidate, comparand);
		}
	},
	RE() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (comparand == null)) { return false; }
			return comparand.toString().matches(candidate.toString());
		}
	},
	NRE() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return !RE.eval(CmfDataType.STRING, candidate, comparand);
		}
	},
	GLOB() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return RE.eval(CmfDataType.STRING, Tools.toString(candidate), Tools.globToRegex(Tools.toString(comparand)));
		}
	},
	NGLOB() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return !GLOB.eval(type, candidate, comparand);
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

	private static final Collection<FastDateFormat> DATE_FORMATS;
	static {
		// First, make sure valueOf() works for every comparison
		for (Comparison c : Comparison.values()) {
			String name = c.name();
			if (name.endsWith("I")) {
				Comparison.valueOf(name.substring(0, name.length() - 1));
			} else {
				Comparison.valueOf(String.format("%sI", name));
			}
		}

		String[] dateFormats = {
			"yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
		};
		List<FastDateFormat> l = new ArrayList<>();
		for (String fmt : dateFormats) {
			l.add(FastDateFormat.getInstance(fmt));
		}
		DATE_FORMATS = Tools.freezeList(l);
	}

	private static int compare(CmfDataType type, Object candidate, Object comparand) {
		switch (type) {
			case BOOLEAN:
				return Tools.compare(Comparison.toBoolean(candidate), Comparison.toBoolean(comparand));

			case DATETIME:
				return Tools.compare(Comparison.toDate(candidate), Comparison.toDate(comparand));

			case DOUBLE:
				return Tools.compare(Comparison.toDouble(candidate), Comparison.toDouble(comparand));

			case INTEGER:
				return Tools.compare(Comparison.toLong(candidate), Comparison.toLong(comparand));

			default:
				return Tools.compare(Tools.toString(candidate), Tools.toString(comparand));
		}
	}

	private static Boolean toBoolean(Object obj) {
		if (obj == null) { return null; }
		if (CmfValue.class.isInstance(obj)) { return CmfValue.class.cast(obj).asBoolean(); }
		if (Boolean.class.isInstance(obj)) { return Boolean.class.cast(obj); }
		if (Number.class.isInstance(obj)) { return (Number.class.cast(obj).intValue() != 0); }
		// Must treat as a string...
		return Tools.toBoolean(obj);
	}

	private static Long toLong(Object obj) {
		if (obj == null) { return null; }
		if (CmfValue.class.isInstance(obj)) { return (long) CmfValue.class.cast(obj).asInteger(); }
		if (Number.class.isInstance(obj)) { return Number.class.cast(obj).longValue(); }
		// Must treat as a string...
		String str = Tools.toTrimmedString(obj, true);
		if (str == null) { return null; }
		return Long.valueOf(str);
	}

	private static Double toDouble(Object obj) {
		if (obj == null) { return null; }
		if (CmfValue.class.isInstance(obj)) { return CmfValue.class.cast(obj).asDouble(); }
		if (Number.class.isInstance(obj)) { return Number.class.cast(obj).doubleValue(); }
		// Must treat as a string...
		String str = Tools.toTrimmedString(obj, true);
		if (str == null) { return null; }
		return Double.valueOf(str);
	}

	private static Date toDate(Object obj) {
		if (obj == null) { return null; }
		if (CmfValue.class.isInstance(obj)) {
			try {
				return CmfValue.class.cast(obj).asTime();
			} catch (ParseException e) {
				// Ignore this... let it be handled by the string mode stuff below
			}
		} else {
			if (Calendar.class.isInstance(obj)) {
				obj = Calendar.class.cast(obj).getTime();
			}
			if (Date.class.isInstance(obj)) { return Date.class.cast(obj); }
		}

		// Treat it as a string
		String str = StringUtils.strip(Tools.toString(obj));
		if (str == null) { return null; }

		for (FastDateFormat fmt : Comparison.DATE_FORMATS) {
			try {
				return fmt.parse(str);
			} catch (ParseException e) {
				// Ignore this...we have more formats to try...
			}
		}
		throw new RuntimeTransformationException(String.format("Could not parse the date string [%s]", str));
	}

	protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
		// The default implementation only looks for the case-insensitive counterpart.
		// This way we only have to provide the comparison implementation assuming case
		// sensitivity.
		String name = name().toLowerCase();
		if (!name.endsWith("i")) { throw new AbstractMethodError(
			String.format("Must provide a concrete implementation for the %s comparison check", name())); }

		// Case-insensitive, find my counterpart!
		Comparison comp = Comparison.valueOf(name.substring(0, name.length() - 1));
		if (type == CmfDataType.STRING) {
			comparand = (comparand != null ? comparand.toString().toUpperCase() : null);
			candidate = (candidate != null ? candidate.toString().toUpperCase() : null);
		}
		return comp.eval(type, candidate, comparand);
	}

	public final boolean check(CmfDataType type, Object candidate, Object comparand) {
		Objects.requireNonNull(comparand, "Must provide a non-null comparand value to check the candidate against");
		return eval(type, candidate, comparand);
	}

	public static Comparison get(String value) {
		return Comparison.get(value, null);
	}

	public static Comparison get(String value, Comparison def) {
		if (value == null) { return def; }
		return Comparison.valueOf(value.toUpperCase());
	}
}