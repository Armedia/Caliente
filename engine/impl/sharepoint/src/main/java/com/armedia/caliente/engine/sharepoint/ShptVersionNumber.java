package com.armedia.caliente.engine.sharepoint;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.Tools;

public final class ShptVersionNumber implements Comparable<ShptVersionNumber> {

	public static final Comparator<ShptVersionNumber> REVERSE_ORDER = new Comparator<ShptVersionNumber>() {
		@Override
		public int compare(ShptVersionNumber a, ShptVersionNumber b) {
			// Compare in reverse order
			return b.compareTo(a);
		}
	};

	private final String string;
	private final int major;
	private final int minor;

	public ShptVersionNumber(int major, int minor) {
		this.major = major;
		this.minor = minor;
		if ((this.major < 0) || (this.minor < 0)) { throw new IllegalArgumentException(String.format(
			"Version numbers may not contain negative components: [%d.%d]", this.major, this.minor)); }
		this.string = String.format("%d.%d", major, minor);
	}

	public ShptVersionNumber(String version) {
		if (version == null) { throw new IllegalArgumentException("Must provide a non-null version"); }
		StrTokenizer tok = new StrTokenizer(version, '.');
		List<String> l = tok.getTokenList();
		if (l.size() != 2) { throw new IllegalArgumentException(String.format(
			"Unsupported version number [%s] - must have exactly two components", version)); }
		this.major = Integer.valueOf(l.get(0));
		this.minor = Integer.valueOf(l.get(1));
		if ((this.major < 0) || (this.minor < 0)) { throw new IllegalArgumentException(String.format(
			"Version numbers may not contain negative components: [%d.%d]", this.major, this.minor)); }
		this.string = version;
	}

	public int getMajor() {
		return this.major;
	}

	public int getMinor() {
		return this.minor;
	}

	@Override
	public String toString() {
		return this.string;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.major, this.minor);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ShptVersionNumber other = ShptVersionNumber.class.cast(obj);
		if (this.major != other.major) { return false; }
		if (this.minor != other.minor) { return false; }
		return true;
	}

	public boolean isSuccessorOf(ShptVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		return (compareTo(other) > 0);
	}

	public boolean isAntecedentOf(ShptVersionNumber other) {
		if (other == null) { throw new IllegalArgumentException("Must provide another version number to check against"); }
		return (compareTo(other) < 0);
	}

	@Override
	public int compareTo(ShptVersionNumber o) {
		// Always sort after NULL
		if (o == null) { return 1; }
		if (this.major != o.major) { return (this.major < o.major ? -1 : 1); }
		if (this.minor != o.minor) { return (this.minor < o.minor ? -1 : 1); }
		return 0;
	}
}