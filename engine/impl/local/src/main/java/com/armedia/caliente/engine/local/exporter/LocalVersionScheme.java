package com.armedia.caliente.engine.local.exporter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.AlphaCounter;
import com.armedia.commons.utilities.Tools;

public class LocalVersionScheme {

	public static Comparator<String> getNumeric() {
		return LocalVersionScheme.getNumeric(null, false);
	}

	public static Comparator<String> getNumeric(Character sep) {
		return LocalVersionScheme.getNumeric(sep, false);
	}

	public static Comparator<String> getNumeric(boolean emptyIsRoot) {
		return LocalVersionScheme.getNumeric(null, emptyIsRoot);
	}

	public static Comparator<String> getNumeric(Character sep, boolean emptyIsRoot) {
		return (a, b) -> LocalVersionScheme.compareNumeric(sep, emptyIsRoot, a, b);
	}

	public static Comparator<String> getAlphanumeric() {
		return LocalVersionScheme.getAlphanumeric(null, false);
	}

	public static Comparator<String> getAlphanumeric(Character sep) {
		return LocalVersionScheme.getAlphanumeric(sep, false);
	}

	public static Comparator<String> getAlphanumeric(boolean emptyIsRoot) {
		return LocalVersionScheme.getAlphanumeric(null, emptyIsRoot);
	}

	public static Comparator<String> getAlphanumeric(Character sep, boolean emptyIsRoot) {
		return (a, b) -> LocalVersionScheme.compareAlphanumeric(sep, emptyIsRoot, a, b);
	}

	public static Comparator<String> getAlphabetic(CharSequence alphabet) {
		return LocalVersionScheme.getAlphabetic(alphabet, null, false);
	}

	public static Comparator<String> getAlphabetic(CharSequence alphabet, Character sep) {
		return LocalVersionScheme.getAlphabetic(alphabet, sep, false);
	}

	public static Comparator<String> getAlphabetic(CharSequence alphabet, boolean emptyIsRoot) {
		return LocalVersionScheme.getAlphabetic(alphabet, null, emptyIsRoot);
	}

	public static Comparator<String> getAlphabetic(CharSequence alphabet, Character sep, boolean emptyIsRoot) {
		return (a, b) -> LocalVersionScheme.compareAlphabetic(alphabet, sep, emptyIsRoot, a, b);
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
		Integer basic = LocalVersionScheme.basicCompare(a, b, emptyIsRoot);
		if (basic != null) { return basic; }

		List<String> A = LocalVersionScheme.split(sep, a);
		List<String> B = LocalVersionScheme.split(sep, b);

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
		return LocalVersionScheme.compare(sep, emptyIsRoot, a, b, LocalVersionScheme.NUMERIC);
	}

	public static final int compareAlphanumeric(Character sep, boolean emptyIsRoot, String a, String b) {
		Comparator<String> c = (A, B) -> {
			final int v = A.compareTo(B);
			if (v == 0) { return v; }
			return v / Math.abs(v);
		};
		return LocalVersionScheme.compare(sep, emptyIsRoot, a, b, c);
	}

	public static final int compareAlphabetic(CharSequence alphabet, Character sep, boolean emptyIsRoot, String a,
		String b) {
		Comparator<String> comparator = (A, B) -> {
			long lA = AlphaCounter.count(A, alphabet);
			long lB = AlphaCounter.count(B, alphabet);
			if (lA == lB) { return 0; }
			return (lA < lB ? -1 : 1);
		};
		return LocalVersionScheme.compare(sep, emptyIsRoot, a, b, comparator);
	}
}