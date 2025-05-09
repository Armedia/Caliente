package com.armedia.caliente.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class AlphaCounter {
	public static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private static class NumberBase {
		private final Map<Character, Integer> digitToValue;
		private final Map<Integer, Character> valueToDigit;

		private NumberBase(CharSequence digits) {
			Map<Character, Integer> digitToValue = new LinkedHashMap<>();
			Map<Integer, Character> valueToDigit = new LinkedHashMap<>();

			final int base = digits.length();
			for (int i = 0; i < digits.length(); i++) {
				char c = digits.charAt(i);
				int v = ((i + 1) % base);
				if (c == '\0') {
					throw new IllegalArgumentException(
						String.format("Illegal digit '\\0' at position %,d in [%s]", i, digits));
				}
				Integer old = digitToValue.put(c, v);
				if (old != null) {
					throw new IllegalArgumentException(String
						.format("Digit [%s] is specified twice with values %d and %d in [%s]", c, old, v, digits));
				}
				valueToDigit.put(v, c);
			}
			this.digitToValue = Tools.freezeMap(digitToValue);
			this.valueToDigit = Tools.freezeMap(valueToDigit);
		}
	}

	private static final NumberBase ALPHABET_BASE = new NumberBase(AlphaCounter.ALPHABET);

	private static final NumberBase getNumberBase(CharSequence digits) {
		if ((AlphaCounter.ALPHABET_BASE != null) && AlphaCounter.ALPHABET.equals(digits)) {
			return AlphaCounter.ALPHABET_BASE;
		}
		return new NumberBase(digits);
	}

	public static String render(long num, CharSequence digits) {
		// The number is 0-based, so bump it by one
		num++;
		if (num <= 0) { return ""; }
		final Map<Integer, Character> index = AlphaCounter.getNumberBase(digits).valueToDigit;
		final int base = index.size();
		StringBuilder sb = new StringBuilder();
		while (num > 0) {
			long digitValue = (num % base);
			num /= base;
			char digit = index.get((int) digitValue);
			if (digitValue == 0) {
				num--;
			}
			sb.append(digit);
		}
		return sb.reverse().toString();
	}

	public static String renderAlpha(long num) {
		return AlphaCounter.render(num, AlphaCounter.ALPHABET);
	}

	public static long count(String str, CharSequence digits) {
		if (StringUtils.isEmpty(str)) { return -1; }

		Map<Character, Integer> digitToValue = AlphaCounter.getNumberBase(digits).digitToValue;

		long ret = 0;
		final int base = digitToValue.size();
		final int chars = str.length();
		final int mult = digitToValue.size();
		for (int i = 0; i < chars; i++) {
			if (i > 0) {
				ret *= mult;
			}
			char c = str.charAt(i);
			if (!digitToValue.containsKey(c)) {
				throw new IllegalArgumentException(String
					.format("Character [%s] in position %,d in [%s] is not a valid alpha counter character", c, i, str));
			}
			int value = digitToValue.get(c);
			ret += (value != 0 ? value : base);
		}
		// Return a 0-based number
		return (ret - 1);
	}

	public static long countAlpha(String str) {
		return AlphaCounter.count(str, AlphaCounter.ALPHABET);
	}
}