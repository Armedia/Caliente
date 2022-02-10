package com.armedia.caliente.store.s3;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public enum S3CharFixer {
	//
	NONE() {
		@Override
		protected void applyFix(StringBuilder b, char c) {
			// DO nothing...
			b.append(c);
		}

		// Optimize for speed...
		@Override
		public String fixCharacters(String str) {
			return str;
		}
	}, //

	ENCODE() {
		private final String encoding = StandardCharsets.UTF_8.name();

		@Override
		protected void applyFix(StringBuilder b, char c) {
			try {
				b.append(URLEncoder.encode(String.valueOf(c), this.encoding).toUpperCase());
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(this.encoding + " encoding is not supported?!?!");
			}
		}
	}, //

	REMOVE() {
		@Override
		protected void applyFix(StringBuilder b, char c) {
			// Do nothing... we're skipping the character
		}
	}, //

	REPLACE() {
		@Override
		protected void applyFix(StringBuilder b, char c) {
			b.append("_");
		}
	}, //
		//
	;

	private static final String CHARS_BAD_STR = "\\{}^%[]`\"~#|<>";
	private static final Set<Character> CHARS_BAD;
	static {
		Set<Character> s = null;

		s = new HashSet<>();
		for (int i = 0; i < S3CharFixer.CHARS_BAD_STR.length(); i++) {
			s.add(S3CharFixer.CHARS_BAD_STR.charAt(i));
		}
		for (int i = 0x80; i <= 0xFF; i++) {
			s.add(Character.valueOf((char) i));
		}
		CHARS_BAD = Tools.freezeSet(s);
	}

	protected abstract void applyFix(StringBuilder b, char c);

	public String fixCharacters(String str) {
		if (StringUtils.isEmpty(str)) { return str; }
		StringBuilder b = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (S3CharFixer.CHARS_BAD.contains(c)) {
				applyFix(b, c);
			} else {
				b.append(c);
			}
		}
		return b.toString();
	}
}