package com.armedia.caliente.cli.filter;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValueFilter;
import com.armedia.commons.utilities.Tools;

public abstract class NumericValueFilter<N extends Number> extends OptionValueFilter implements Comparator<N> {

	private final N min;
	private final boolean minInc;
	private final N max;
	private final boolean maxInc;
	private final String description;

	protected NumericValueFilter(String label, N min, N max) {
		this(label, min, true, max, true);

	}

	protected NumericValueFilter(String label, N min, boolean minInclusive, N max, boolean maxInclusive) {
		this.min = min;
		this.minInc = minInclusive;
		this.max = max;
		this.maxInc = maxInclusive;
		String prefix = "a";
		if (StringUtils.startsWithAny(label.toLowerCase(), "a", "e", "i", "o", "u")) {
			prefix = "an";
		}
		String lorange = null;
		if (min != null) {
			lorange = String.format(" greater than%s %s", (minInclusive ? " or equal to" : ""), min);
		}
		String hirange = null;
		if (max != null) {
			hirange = String.format(" less than%s %s", (maxInclusive ? " or equal to" : ""), max);
		}

		String range = null;
		if ((lorange != null) && (hirange != null)) {
			// We have to render a chained range...
			range = String.format("%s, and %s", lorange, hirange);
		} else {
			// Pick the first non-null, since we don't have to chain any of them...include the empty
			// string to cover for the case when both are null (i.e. no range)
			range = Tools.coalesce(lorange, hirange, "");
		}

		this.description = String.format("%s %s number%s", prefix, label, range);
	}

	protected abstract N convert(String str) throws NumberFormatException;

	protected boolean isProper(N value) {
		return (value != null);
	}

	private final boolean isInRange(N n) {
		// If no min, always be higher than the minimum
		final int lo = (this.min != null ? compare(this.min, n) : 1);
		// If no max, always be lower than the maximum
		final int hi = (this.max != null ? compare(n, this.max) : -1);

		boolean ok = true;
		if (ok && (this.min != null)) {
			if (this.minInc) {
				ok &= (lo >= 0);
			} else {
				ok &= (lo > 0);
			}
		}

		if (ok && (this.max != null)) {
			if (this.maxInc) {
				ok &= (0 >= hi);
			} else {
				ok &= (0 > hi);
			}
		}

		return ok;
	}

	@Override
	protected final boolean checkValue(String value) {
		try {
			// 1) Is it a number?
			final N n = convert(value);
			// 2) Is it a proper value in the required range?
			return isProper(n) && isInRange(n);
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public final String getDefinition() {
		return this.description;
	}
}