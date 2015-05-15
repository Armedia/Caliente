package com.armedia.cmf.storage.tools;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.armedia.commons.utilities.Tools;

public final class FilenameFixer {

	private static final String ENCODING = "UTF-8";

	private static final String LIN_INVALID_CHARS = "/\0";
	private static final Map<Character, String> LIN_ENCODER;

	private static final String WIN_INVALID_CHARS = "<>:\\|?*";
	private static final Map<Character, String> WIN_ENCODER;

	static {
		Map<Character, String> m = new HashMap<Character, String>();
		for (int i = 0; i < FilenameFixer.LIN_INVALID_CHARS.length(); i++) {
			final char c = FilenameFixer.LIN_INVALID_CHARS.charAt(i);
			m.put(Character.valueOf(c), String.format("%%%02X", (int) c));
		}
		LIN_ENCODER = Tools.freezeMap(m);

		m = new HashMap<Character, String>();
		for (int i = 0; i < FilenameFixer.WIN_INVALID_CHARS.length(); i++) {
			final char c = FilenameFixer.WIN_INVALID_CHARS.charAt(i);
			m.put(Character.valueOf(c), String.format("%%%02X", (int) c));
		}
		// 0x01, 0x02, ... 0x1F
		for (int i = 0; i < 32; i++) {
			m.put(Character.valueOf((char) i), String.format("%%%02X", i));
		}
		WIN_ENCODER = Tools.freezeMap(m);
	}

	public static String safeEncodeChar(Character c, boolean forWindows) {
		String str = FilenameFixer.LIN_ENCODER.get(c);
		if (str != null) { return str; }
		if (forWindows) {
			str = FilenameFixer.WIN_ENCODER.get(c);
			if (str != null) { return str; }
		}
		return c.toString();
	}

	public static String safeEncode(String str, boolean forWindows) {
		StringBuilder b = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			b.append(FilenameFixer.safeEncodeChar(str.charAt(i), forWindows));
		}
		str = b.toString();

		if (forWindows) {
			str = str.replaceAll("([\\.\\s])$", "$1_"); // Can't end with a dot or a space
			str = str.replaceAll("^(\\\\s)", "_$1"); // Can't begin with a space

			// Also invalid are CON, PRN, AUX, NUL, COM[1-9], LPT[1-9], CLOCK$
		}
		return str;
	}

	public static String urlEncode(String str) {
		try {
			return URLEncoder.encode(str, FilenameFixer.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(
				String.format("%s encoding isn't supported in this JVM", FilenameFixer.ENCODING), e);
		}
	}

	public static String urlDecode(String str) {
		try {
			return URLDecoder.decode(str, FilenameFixer.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(
				String.format("%s encoding isn't supported in this JVM", FilenameFixer.ENCODING), e);
		}
	}

	private FilenameFixer() {
	}
}