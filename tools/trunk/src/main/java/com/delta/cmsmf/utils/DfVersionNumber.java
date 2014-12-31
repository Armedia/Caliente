package com.delta.cmsmf.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.Tools;

public final class DfVersionNumber implements Comparable<DfVersionNumber> {
	private final String string;
	private final int[] numbers;

	public DfVersionNumber(String version) {
		StrTokenizer tok = new StrTokenizer(version, '.');
		List<String> l = tok.getTokenList();
		this.numbers = new int[l.size()];
		int i = 0;
		for (String str : l) {
			this.numbers[i++] = Integer.valueOf(str);
		}
		this.string = version;
	}

	public int getComponentCount() {
		return this.numbers.length;
	}

	public String asString() {
		return this.string;
	}

	public String asString(int components) {
		StringBuilder b = new StringBuilder();
		components = Math.max(components, this.numbers.length);
		for (int i = 0; i < components; i++) {
			if (i > 0) {
				b.append('.');
			}
			b.append(String.valueOf(i < this.numbers.length ? this.numbers[i] : 0));
		}
		return b.toString();
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.numbers);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		DfVersionNumber other = DfVersionNumber.class.cast(obj);
		return Arrays.equals(this.numbers, other.numbers);
	}

	public boolean equals(Object obj, int depth) {
		if (depth < 0) { throw new IllegalArgumentException("Must provide a positive depth"); }
		if (!Tools.baseEquals(this, obj)) { return false; }
		DfVersionNumber other = DfVersionNumber.class.cast(obj);
		if (depth == 0) {
			// Make sure that if the depth is 0, then the equals will
			// compare ALL components up to the shortest of the two
			// version number sets
			depth = Integer.MAX_VALUE;
		}
		final int limit = Tools.min(depth, this.numbers.length, other.numbers.length);
		for (int i = 0; i < limit; i++) {
			final int a;
			if (i < this.numbers.length) {
				a = this.numbers[i];
			} else {
				a = 0;
			}
			final int b;
			if (i < other.numbers.length) {
				b = other.numbers[i];
			} else {
				b = 0;
			}
			if (a != b) { return false; }
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("DfVersionNumber %s [%s]", this.string, Arrays.toString(this.numbers));
	}

	private boolean isSameBranch(DfVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		final int length = getComponentCount();
		if (length != other.getComponentCount()) { return false; }
		return this.equals(other, length - 1);
	}

	public boolean isSuccessorOf(DfVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		final int length = getComponentCount();
		if (length != other.getComponentCount()) { return false; }
		if (length == 2) {
			// We must sort behind the other
			return (compareTo(other) > 0);
		} else {
			// All but the last one must be equal, and the last one must be higher
			// than other's
			if (!isSameBranch(other)) { return false; }
			return this.numbers[length - 1] > other.numbers[length - 1];
		}
	}

	public boolean isAntecedentOf(DfVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		final int length = getComponentCount();
		if (length != other.getComponentCount()) { return false; }
		if (length == 2) {
			// We must sort ahead of the other
			return (compareTo(other) < 0);
		} else {
			// All but the last one must be equal, and the last one must be lower
			// than other's
			if (!isSameBranch(other)) { return false; }
			return this.numbers[length - 1] < other.numbers[length - 1];
		}
	}

	public boolean isAncestorOf(DfVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		if (getComponentCount() >= other.getComponentCount()) { return false; }
		return equals(other, 0);
	}

	public boolean isDescendantOf(DfVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		if (getComponentCount() <= other.getComponentCount()) { return false; }
		return equals(other, 0);
	}

	public int getDepthInCommon(DfVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		int length = getComponentCount();
		for (int i = 0; i < length; i++) {
			if (i >= other.numbers.length) { return i; }
			if (this.numbers[i] != other.numbers[i]) { return i; }
		}
		return length;
	}

	@Override
	public int compareTo(DfVersionNumber o) {
		// Always sort after NULL
		if (o == null) { return 1; }
		final int n = Math.max(this.numbers.length, o.numbers.length);
		for (int i = 0; i < n; i++) {
			final int a;
			if (i < this.numbers.length) {
				a = this.numbers[i];
			} else {
				a = 0;
			}
			final int b;
			if (i < o.numbers.length) {
				b = o.numbers[i];
			} else {
				b = 0;
			}
			if (a < b) { return -1; }
			if (a > b) { return 1; }
		}
		return 0;
	}
}