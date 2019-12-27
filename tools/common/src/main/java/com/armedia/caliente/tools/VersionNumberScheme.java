package com.armedia.caliente.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class VersionNumberScheme implements Comparator<String> {

	public static final String NUM_ALPHABET = "0123456789";
	public static final Comparator<String> NUM = (a, b) -> {
		int A = Integer.valueOf(a);
		int B = Integer.valueOf(b);
		if (A == B) { return 0; }
		return (A < B ? -1 : 1);
	};

	public static final String ALPHA_ALPHABET = AlphaCounter.ALPHABET;

	public static final String ALNUM_ALPHABET = VersionNumberScheme.NUM_ALPHABET + VersionNumberScheme.ALPHA_ALPHABET;
	public static final Comparator<String> ALNUM = (a, b) -> {
		final int v = a.compareTo(b);
		if (v == 0) { return v; }
		return v / Math.abs(v);
	};

	private final boolean emptyIsRoot;
	private final Character sep;
	private final CharSequence alphabet;
	private final Comparator<String> elementComparator;

	public VersionNumberScheme(Character sep, boolean emptyIsRoot, CharSequence alphabet,
		Comparator<String> elementComparator) {
		this.emptyIsRoot = emptyIsRoot;
		this.sep = sep;
		this.alphabet = alphabet;
		this.elementComparator = elementComparator;
	}

	public boolean isEmptyIsRoot() {
		return this.emptyIsRoot;
	}

	public Character getSep() {
		return this.sep;
	}

	public CharSequence getAlphabet() {
		return this.alphabet;
	}

	public Comparator<String> getElementComparator() {
		return this.elementComparator;
	}

	private List<String> split(Character sep, String s) {
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

	@Override
	public final int compare(String a, String b) {
		// Basic comparisons
		Integer basic = VersionNumberScheme.basicCompare(a, b, this.emptyIsRoot);
		if (basic != null) { return basic; }

		List<String> A = split(this.sep, a);
		List<String> B = split(this.sep, b);

		int max = Math.min(A.size(), B.size());
		for (int i = 0; i < max; i++) {
			int r = this.elementComparator.compare(A.get(i), B.get(i));
			if (r != 0) { return r; }
		}

		// All elements were compared, and no difference was found...
		if (A.size() == B.size()) { return 0; }

		// There are elements left, they must be in only one of the two, and that one - the longer
		// one - will sort after the shorter one
		return (A.size() == max ? -1 : 1);
	}

	public static VersionNumberScheme getNumeric() {
		return VersionNumberScheme.getNumeric(null, false);
	}

	public static VersionNumberScheme getNumeric(Character sep) {
		return VersionNumberScheme.getNumeric(sep, false);
	}

	public static VersionNumberScheme getNumeric(boolean emptyIsRoot) {
		return VersionNumberScheme.getNumeric(null, emptyIsRoot);
	}

	public static VersionNumberScheme getNumeric(Character sep, boolean emptyIsRoot) {
		return new VersionNumberScheme(sep, emptyIsRoot, VersionNumberScheme.NUM_ALPHABET, VersionNumberScheme.NUM);
	}

	public static VersionNumberScheme getAlphanumeric() {
		return VersionNumberScheme.getAlphanumeric(null, false);
	}

	public static VersionNumberScheme getAlphanumeric(Character sep) {
		return VersionNumberScheme.getAlphanumeric(sep, false);
	}

	public static VersionNumberScheme getAlphanumeric(boolean emptyIsRoot) {
		return VersionNumberScheme.getAlphanumeric(null, emptyIsRoot);
	}

	public static VersionNumberScheme getAlphanumeric(Character sep, boolean emptyIsRoot) {
		return new VersionNumberScheme(sep, emptyIsRoot, VersionNumberScheme.ALNUM_ALPHABET, VersionNumberScheme.ALNUM);
	}

	public static VersionNumberScheme getAlphabetic(CharSequence alphabet) {
		return VersionNumberScheme.getAlphabetic(alphabet, null, false);
	}

	public static VersionNumberScheme getAlphabetic(CharSequence alphabet, Character sep) {
		return VersionNumberScheme.getAlphabetic(alphabet, sep, false);
	}

	public static VersionNumberScheme getAlphabetic(CharSequence alphabet, boolean emptyIsRoot) {
		return VersionNumberScheme.getAlphabetic(alphabet, null, emptyIsRoot);
	}

	public static VersionNumberScheme getAlphabetic(CharSequence alphabet, Character sep, boolean emptyIsRoot) {
		Objects.requireNonNull(alphabet, "Must provide a CharSequence to use as the alphabet");
		Comparator<String> comparator = (A, B) -> {
			long lA = AlphaCounter.count(A, alphabet);
			long lB = AlphaCounter.count(B, alphabet);
			if (lA == lB) { return 0; }
			return (lA < lB ? -1 : 1);
		};
		return new VersionNumberScheme(sep, emptyIsRoot, alphabet, comparator);
	}
}