package com.delta.cmsmf.utils;

import java.util.List;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.Tools;

public final class DfVersionNumber implements Comparable<DfVersionNumber> {

	private static String toString(int[] numbers, int length) {
		if (length < 0) { throw new IllegalArgumentException("Must provide a positive length"); }
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				b.append('.');
			}
			b.append(String.valueOf(i < numbers.length ? numbers[i] : 0));
		}
		return b.toString();
	}

	private final String string;
	private final int[] numbers;
	private final int length;

	private DfVersionNumber(int[] numbers, int length) {
		this.numbers = numbers;
		this.length = length;
		this.string = DfVersionNumber.toString(numbers, length);
	}

	public DfVersionNumber(String version) {
		StrTokenizer tok = new StrTokenizer(version, '.');
		List<String> l = tok.getTokenList();
		this.numbers = new int[l.size()];
		int i = 0;
		for (String str : l) {
			this.numbers[i++] = Integer.valueOf(str);
		}
		this.string = version;
		this.length = this.numbers.length;
	}

	public int getComponentCount() {
		return this.length;
	}

	@Override
	public String toString() {
		return this.string;
	}

	public String toString(int components) {
		return DfVersionNumber.toString(this.numbers, components);
	}

	@Override
	public int hashCode() {
		int[] arr = new int[this.length];
		System.arraycopy(this.numbers, 0, arr, 0, arr.length);
		return Tools.hashTool(this, null, this.length, arr);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		DfVersionNumber other = DfVersionNumber.class.cast(obj);
		if (getComponentCount() != other.getComponentCount()) { return false; }
		return equals(other, 0);
	}

	public boolean equals(DfVersionNumber other, int depth) {
		if (depth < 0) { throw new IllegalArgumentException("Must provide a positive depth"); }
		if (other == this) { return true; }
		final int thisLength = getComponentCount();
		final int otherLength = other.getComponentCount();
		int limit = Math.max(thisLength, otherLength);
		if (depth > 0) {
			limit = Tools.min(depth, limit);
		}
		for (int i = 0; i < limit; i++) {
			final int a;
			if (i < thisLength) {
				a = this.numbers[i];
			} else {
				a = 0;
			}
			final int b;
			if (i < otherLength) {
				b = other.numbers[i];
			} else {
				b = 0;
			}
			if (a != b) { return false; }
		}
		return true;
	}

	public DfVersionNumber getSubset(int components) {
		if (components < 1) { throw new IllegalArgumentException("Must contain at least one version component"); }
		if (components >= getComponentCount()) { return this; }
		return new DfVersionNumber(this.numbers, components);
	}

	private boolean isSameBranch(DfVersionNumber other) {
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
		int len = getComponentCount();
		if (len >= other.getComponentCount()) { return false; }
		return equals(other, len);
	}

	public boolean isDescendantOf(DfVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		int len = other.getComponentCount();
		if (getComponentCount() <= len) { return false; }
		return equals(other, len);
	}

	public int getDepthInCommon(DfVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		final int length = getComponentCount();
		final int otherLength = other.getComponentCount();
		for (int i = 0; i < length; i++) {
			if (i >= otherLength) { return i; }
			if (this.numbers[i] != other.numbers[i]) { return i; }
		}
		return length;
	}

	@Override
	public int compareTo(DfVersionNumber o) {
		// Always sort after NULL
		if (o == null) { return 1; }
		final int thisLength = getComponentCount();
		final int otherLength = o.getComponentCount();
		final int n = Math.max(thisLength, otherLength);
		for (int i = 0; i < n; i++) {
			final int a;
			if (i < thisLength) {
				a = this.numbers[i];
			} else {
				a = 0;
			}
			final int b;
			if (i < otherLength) {
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