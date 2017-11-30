package com.armedia.caliente.engine.dynamic.xml;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.RuntimeDynamicElementException;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public enum Comparison {
	// Base checks
	EQ() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand, boolean caseInsensitive) {
			candidate = Comparison.canonicalizeCase(type, candidate, caseInsensitive);
			comparand = Comparison.canonicalizeCase(type, comparand, caseInsensitive);
			return (Comparison.compare(type, candidate, comparand) == 0);
		}
	},
	GT() {
		@Override
		protected boolean eval(CmfDataType type, Object bigger, Object smaller, boolean caseInsensitive) {
			bigger = Comparison.canonicalizeCase(type, bigger, caseInsensitive);
			smaller = Comparison.canonicalizeCase(type, smaller, caseInsensitive);
			return (Comparison.compare(type, bigger, smaller) > 0);
		}
	},
	GE() {
		@Override
		protected boolean eval(CmfDataType type, Object bigger, Object smallerOrEqual, boolean caseInsensitive) {
			bigger = Comparison.canonicalizeCase(type, bigger, caseInsensitive);
			smallerOrEqual = Comparison.canonicalizeCase(type, smallerOrEqual, caseInsensitive);
			return (Comparison.compare(type, bigger, smallerOrEqual) >= 0);
		}
	},
	LT() {
		@Override
		protected boolean eval(CmfDataType type, Object smaller, Object bigger, boolean caseInsensitive) {
			return GT.eval(type, bigger, smaller, caseInsensitive);
		}
	},
	LE() {
		@Override
		protected boolean eval(CmfDataType type, Object smallerOrEqual, Object bigger, boolean caseInsensitive) {
			return GE.eval(type, bigger, smallerOrEqual, caseInsensitive);
		}
	},
	SW() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object prefix, boolean caseInsensitive) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (prefix == null)) { return false; }
			candidate = Comparison.canonicalizeCase(type, candidate, caseInsensitive);
			prefix = Comparison.canonicalizeCase(type, prefix, caseInsensitive);
			return candidate.toString().startsWith(prefix.toString());
		}
	},
	EW() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object suffix, boolean caseInsensitive) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (suffix == null)) { return false; }
			candidate = Comparison.canonicalizeCase(type, candidate, caseInsensitive);
			suffix = Comparison.canonicalizeCase(type, suffix, caseInsensitive);
			return candidate.toString().endsWith(suffix.toString());
		}
	},
	CN() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object substring, boolean caseInsensitive) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (substring == null)) { return false; }
			candidate = Comparison.canonicalizeCase(type, candidate, caseInsensitive);
			substring = Comparison.canonicalizeCase(type, substring, caseInsensitive);
			return (candidate.toString().indexOf(substring.toString()) >= 0);
		}
	},
	RE() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object regex, boolean caseInsensitive) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (regex == null)) { return false; }
			int flags = (caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
			return Pattern.compile(regex.toString(), flags).matcher(candidate.toString()).find();
		}
	},
	GLOB() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand, boolean caseInsensitive) {
			return RE.eval(CmfDataType.STRING, Tools.toString(candidate), Tools.globToRegex(Tools.toString(comparand)),
				caseInsensitive);
		}
	},

	// Case-insensitive checks
	EQI(), //
	GTI(), //
	GEI(), //
	LTI(), //
	LEI(), //
	SWI(), //
	EWI(), //
	CNI(), //
	REI(), //
	GLOBI(), //

	// Negative checks
	NEQ(), //
	NGT(), //
	NGE(), //
	NLT(), //
	NLE(), //
	NSW(), //
	NEW(), //
	NCN(), //
	NRE(), //
	NGLOB(), //

	// Negative case-insensitive checks
	NEQI(), //
	NGTI(), //
	NGEI(), //
	NLTI(), //
	NLEI(), //
	NSWI(), //
	NEWI(), //
	NCNI(), //
	NREI(), //
	NGLOBI(), //
	//
	;

	private static Object canonicalizeCase(CmfDataType type, Object value, boolean caseInsensitive) {
		if ((value == null) || (type != CmfDataType.STRING) || !caseInsensitive) { return value; }
		return StringUtils.upperCase(String.valueOf(value));
	}

	public static Comparison DEFAULT = Comparison.EQ;

	public static int compare(CmfDataType type, Object candidate, Object comparand) {
		switch (type) {
			case BOOLEAN:
				return Tools.compare(Comparison.toBoolean(candidate), Comparison.toBoolean(comparand));

			case DATETIME:
				return Tools.compare(Comparison.toDate(candidate), Comparison.toDate(comparand));

			case DOUBLE:
				return Tools.compare(Comparison.toDouble(candidate), Comparison.toDouble(comparand));

			case INTEGER:
				return Tools.compare(Comparison.toLong(candidate), Comparison.toLong(comparand));

			case URI:
				return Tools.compare(Comparison.toURI(candidate), Comparison.toURI(comparand));

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

		try {
			return new CmfValue(CmfDataType.DATETIME, str).asTime();
		} catch (ParseException e) {
			throw new RuntimeDynamicElementException(String.format("Could not parse the date string [%s]", str));
		}
	}

	private static URI toURI(Object obj) {
		if (obj == null) { return null; }
		if (File.class.isInstance(obj)) { return File.class.cast(obj).toURI(); }
		if (Path.class.isInstance(obj)) { return Path.class.cast(obj).toUri(); }
		if (URI.class.isInstance(obj)) { return URI.class.cast(obj); }

		if (CmfValue.class.isInstance(obj)) {
			obj = CmfValue.class.cast(obj).asObject();
		}

		final String str = obj.toString();
		try {
			if (URL.class.isInstance(obj)) { return URL.class.cast(obj).toURI(); }
			return new URI(str);
		} catch (URISyntaxException e) {
			throw new RuntimeDynamicElementException(String.format("Could not parse the URI string [%s]", str), e);
		}
	}

	protected boolean eval(CmfDataType type, Object candidate, Object comparand, boolean caseInsensitive) {
		// The default implementation only looks for the case-insensitive counterpart.
		// This way we only have to provide the comparison implementation assuming case
		// sensitivity.
		String name = name().toUpperCase();
		if (!name.startsWith("N") && !name.endsWith("I")) { throw new AbstractMethodError(
			String.format("Must provide a concrete implementation of eval() for the %s comparison check", name())); }

		// Case-insensitive, find my counterpart!
		boolean negated = false;
		caseInsensitive = false;
		if (name.startsWith("N")) {
			negated = true;
			name = name.substring(1);
		}
		if (name.endsWith("I")) {
			name = name.substring(0, name.length() - 1);
			caseInsensitive = true;
		}
		Comparison comp = Comparison.valueOf(name);
		return (negated ^ comp.eval(type, candidate, comparand, caseInsensitive));
	}

	public final boolean check(CmfDataType type, Object candidate, Object comparand) {
		Objects.requireNonNull(comparand, "Must provide a non-null comparand value to check the candidate against");
		return eval(type, candidate, comparand, false);
	}

	public static Comparison get(String value) {
		return Comparison.get(value, null);
	}

	public static Comparison get(String value, Comparison def) {
		if (value == null) { return def; }
		return Comparison.valueOf(value.toUpperCase());
	}
}