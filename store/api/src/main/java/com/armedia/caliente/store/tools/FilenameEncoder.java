/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.store.tools;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.armedia.commons.utilities.Tools;

public final class FilenameEncoder {

	private static final String ENCODING = "UTF-8";

	private static final String LIN_INVALID_CHARS = "/\0";
	private static final Map<Character, String> LIN_ENCODER;

	private static final String WIN_INVALID_CHARS = "<>:\\|?*\"";
	private static final Map<Character, String> WIN_ENCODER;

	static {
		Map<Character, String> m = new HashMap<>();
		for (int i = 0; i < FilenameEncoder.LIN_INVALID_CHARS.length(); i++) {
			final char c = FilenameEncoder.LIN_INVALID_CHARS.charAt(i);
			m.put(Character.valueOf(c), String.format("%%%02X", (int) c));
		}
		LIN_ENCODER = Tools.freezeMap(m);

		m = new HashMap<>();
		for (int i = 0; i < FilenameEncoder.WIN_INVALID_CHARS.length(); i++) {
			final char c = FilenameEncoder.WIN_INVALID_CHARS.charAt(i);
			m.put(Character.valueOf(c), String.format("%%%02X", (int) c));
		}
		// 0x01, 0x02, ... 0x1F
		for (int i = 0; i < 32; i++) {
			m.put(Character.valueOf((char) i), String.format("%%%02X", i));
		}
		WIN_ENCODER = Tools.freezeMap(m);
	}

	public static String safeEncodeChar(Character c, boolean forWindows) {
		String str = FilenameEncoder.LIN_ENCODER.get(c);
		if (str != null) { return str; }
		if (forWindows) {
			str = FilenameEncoder.WIN_ENCODER.get(c);
			if (str != null) { return str; }
		}
		return c.toString();
	}

	public static String safeEncode(String str, boolean forWindows) {
		StringBuilder b = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			b.append(FilenameEncoder.safeEncodeChar(str.charAt(i), forWindows));
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
			return URLEncoder.encode(str, FilenameEncoder.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(
				String.format("%s encoding isn't supported in this JVM", FilenameEncoder.ENCODING), e);
		}
	}

	public static String urlDecode(String str) {
		try {
			return URLDecoder.decode(str, FilenameEncoder.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(
				String.format("%s encoding isn't supported in this JVM", FilenameEncoder.ENCODING), e);
		}
	}

	private FilenameEncoder() {
	}
}
