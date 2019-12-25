package com.armedia.caliente.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class VersionNumberScheme {

	public static Comparator<String> getNumeric() {
		return VersionNumberScheme.getNumeric(null, false);
	}

	public static Comparator<String> getNumeric(Character sep) {
		return VersionNumberScheme.getNumeric(sep, false);
	}

	public static Comparator<String> getNumeric(boolean emptyIsRoot) {
		return VersionNumberScheme.getNumeric(null, emptyIsRoot);
	}

	public static Comparator<String> getNumeric(Character sep, boolean emptyIsRoot) {
		return (a, b) -> VersionNumberScheme.compareNumeric(sep, emptyIsRoot, a, b);
	}

	public static Comparator<String> getAlphanumeric() {
		return VersionNumberScheme.getAlphanumeric(null, false);
	}

	public static Comparator<String> getAlphanumeric(Character sep) {
		return VersionNumberScheme.getAlphanumeric(sep, false);
	}

	public static Comparator<String> getAlphanumeric(boolean emptyIsRoot) {
		return VersionNumberScheme.getAlphanumeric(null, emptyIsRoot);
	}

	public static Comparator<String> getAlphanumeric(Character sep, boolean emptyIsRoot) {
		return (a, b) -> VersionNumberScheme.compareAlphanumeric(sep, emptyIsRoot, a, b);
	}

	public static Comparator<String> getAlphabetic(CharSequence alphabet) {
		return VersionNumberScheme.getAlphabetic(alphabet, null, false);
	}

	public static Comparator<String> getAlphabetic(CharSequence alphabet, Character sep) {
		return VersionNumberScheme.getAlphabetic(alphabet, sep, false);
	}

	public static Comparator<String> getAlphabetic(CharSequence alphabet, boolean emptyIsRoot) {
		return VersionNumberScheme.getAlphabetic(alphabet, null, emptyIsRoot);
	}

	public static Comparator<String> getAlphabetic(CharSequence alphabet, Character sep, boolean emptyIsRoot) {
		return (a, b) -> VersionNumberScheme.compareAlphabetic(alphabet, sep, emptyIsRoot, a, b);
	}

	protected static List<String> split(Character sep, String s) {
		if (sep != null) { return Tools.splitEscaped(sep, s); }
		List<String> ret = new ArrayList<>(1);
		ret.add(s);
		return ret;
	}

	protected static Integer basicCompare(String a, String b, boolean emptyIsRoot) {
		if (a == b) { return 0; }
		boolean eA = StringUtils.isEmpty(a);
		boolean eB = StringUtils.isEmpty(b);
		if (eA && eB) { return 0; }
		if (eA) { return (emptyIsRoot ? -1 : 1); }
		if (eB) { return (emptyIsRoot ? 1 : -1); }
		if (Objects.equals(a, b)) { return 0; }
		return null;
	}

	private static final Comparator<String> NUMERIC = (a, b) -> {
		int A = Integer.valueOf(a);
		int B = Integer.valueOf(b);
		if (A == B) { return 0; }
		return (A < B ? -1 : 1);
	};

	public static final int compare(Character sep, boolean emptyIsRoot, String a, String b,
		Comparator<String> comparator) {
		// Basic comparisons
		Integer basic = VersionNumberScheme.basicCompare(a, b, emptyIsRoot);
		if (basic != null) { return basic; }

		List<String> A = VersionNumberScheme.split(sep, a);
		List<String> B = VersionNumberScheme.split(sep, b);

		int max = Math.min(A.size(), B.size());
		for (int i = 0; i < max; i++) {
			int r = comparator.compare(A.get(i), B.get(i));
			if (r != 0) { return r; }
		}

		// All elements were compared, and no difference was found...
		if (A.size() == B.size()) { return 0; }

		// There are elements left, they must be in only one of the two, and that one - the longer
		// one - will sort after the shorter one
		return (A.size() == max ? -1 : 1);
	}

	public static final int compareNumeric(Character sep, boolean emptyIsRoot, String a, String b) {
		return VersionNumberScheme.compare(sep, emptyIsRoot, a, b, VersionNumberScheme.NUMERIC);
	}

	public static final int compareAlphanumeric(Character sep, boolean emptyIsRoot, String a, String b) {
		Comparator<String> c = (A, B) -> {
			final int v = A.compareTo(B);
			if (v == 0) { return v; }
			return v / Math.abs(v);
		};
		return VersionNumberScheme.compare(sep, emptyIsRoot, a, b, c);
	}

	public static final int compareAlphabetic(CharSequence alphabet, Character sep, boolean emptyIsRoot, String a,
		String b) {
		Comparator<String> comparator = (A, B) -> {
			long lA = AlphaCounter.count(A, alphabet);
			long lB = AlphaCounter.count(B, alphabet);
			if (lA == lB) { return 0; }
			return (lA < lB ? -1 : 1);
		};
		return VersionNumberScheme.compare(sep, emptyIsRoot, a, b, comparator);
	}
}