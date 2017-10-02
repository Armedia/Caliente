package com.armedia.caliente.engine.transform.xml;

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

import com.armedia.caliente.engine.transform.RuntimeTransformationException;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public enum Comparison {
	// Base checks
	EQ() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return (Comparison.compare(type, candidate, comparand) == 0);
		}
	},
	GT() {
		@Override
		protected boolean eval(CmfDataType type, Object bigger, Object smaller) {
			return (Comparison.compare(type, bigger, smaller) > 0);
		}
	},
	GE() {
		@Override
		protected boolean eval(CmfDataType type, Object bigger, Object smallerOrEqual) {
			return (Comparison.compare(type, bigger, smallerOrEqual) >= 0);
		}
	},
	LT() {
		@Override
		protected boolean eval(CmfDataType type, Object smaller, Object bigger) {
			return GT.eval(type, bigger, smaller);
		}
	},
	LE() {
		@Override
		protected boolean eval(CmfDataType type, Object smallerOrEqual, Object bigger) {
			return GE.eval(type, bigger, smallerOrEqual);
		}
	},
	SW() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object prefix) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (prefix == null)) { return false; }
			return candidate.toString().startsWith(prefix.toString());
		}
	},
	EW() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object suffix) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (suffix == null)) { return false; }
			return candidate.toString().endsWith(suffix.toString());
		}
	},
	CN() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object substring) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (substring == null)) { return false; }
			return (candidate.toString().indexOf(substring.toString()) >= 0);
		}
	},
	RE() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object regex) {
			// Regardless of type, must treat them as strings
			if ((candidate == null) || (regex == null)) { return false; }
			return Pattern.compile(regex.toString()).matcher(candidate.toString()).find();
		}
	},
	GLOB() {
		@Override
		protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
			return RE.eval(CmfDataType.STRING, Tools.toString(candidate), Tools.globToRegex(Tools.toString(comparand)));
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

	public static Comparison DEFAULT = Comparison.EQI;

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
			throw new RuntimeTransformationException(String.format("Could not parse the date string [%s]", str));
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
			throw new RuntimeTransformationException(String.format("Could not parse the URI string [%s]", str), e);
		}
	}

	protected boolean eval(CmfDataType type, Object candidate, Object comparand) {
		// The default implementation only looks for the case-insensitive counterpart.
		// This way we only have to provide the comparison implementation assuming case
		// sensitivity.
		String name = name().toUpperCase();
		if (!name.startsWith("N") && !name.endsWith("I")) { throw new AbstractMethodError(
			String.format("Must provide a concrete implementation of eval() for the %s comparison check", name())); }

		// Case-insensitive, find my counterpart!
		boolean negated = false;
		boolean caseInsensitive = false;
		if (name.startsWith("N")) {
			negated = true;
			name = name.substring(1);
		}
		if (name.endsWith("I")) {
			name = name.substring(0, name.length() - 1);
			caseInsensitive = true;
		}
		Comparison comp = Comparison.valueOf(name);
		if (caseInsensitive && (type == CmfDataType.STRING)) {
			comparand = (comparand != null ? comparand.toString().toUpperCase() : null);
			candidate = (candidate != null ? candidate.toString().toUpperCase() : null);
		}
		boolean result = comp.eval(type, candidate, comparand);
		if (negated) {
			// Flip the result...
			result = !result;
		}
		return result;
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