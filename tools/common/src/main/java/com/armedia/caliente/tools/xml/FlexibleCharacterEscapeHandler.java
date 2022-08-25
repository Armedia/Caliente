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
package com.armedia.caliente.tools.xml;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ConcurrentTools;
import com.sun.xml.bind.marshaller.CharacterEscapeHandler;

public class FlexibleCharacterEscapeHandler implements CharacterEscapeHandler {

	private static final Set<String> NAMES;
	static {
		Set<String> s = new TreeSet<>();
		s.add("com.sun.xml.bind.characterEscapeHandler");
		s.add("com.sun.xml.bind.marshaller.CharacterEscapeHandler");
		NAMES = Tools.freezeSet(s);
	}

	private static final ConcurrentMap<String, FlexibleCharacterEscapeHandler> INSTANCES = new ConcurrentHashMap<>();

	public static FlexibleCharacterEscapeHandler getInstance(String charsetName) {
		return FlexibleCharacterEscapeHandler.getInstance(charsetName,
			FlexibleCharacterEscapeHandler.DEFAULT_ENCODE_INVALID);
	}

	public static FlexibleCharacterEscapeHandler getInstance(Charset charset) {
		return FlexibleCharacterEscapeHandler.getInstance(charset,
			FlexibleCharacterEscapeHandler.DEFAULT_ENCODE_INVALID);
	}

	public static FlexibleCharacterEscapeHandler getInstance(String charsetName, boolean encodeInvalid) {
		return FlexibleCharacterEscapeHandler.getInstance(charsetName, encodeInvalid,
			FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED);
	}

	public static FlexibleCharacterEscapeHandler getInstance(Charset charset, boolean encodeInvalid) {
		return FlexibleCharacterEscapeHandler.getInstance(charset, encodeInvalid,
			FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED);
	}

	public static FlexibleCharacterEscapeHandler getInstance(String charsetName, boolean encodeInvalid,
		boolean excludeDiscouraged) {
		return FlexibleCharacterEscapeHandler.getInstance(
			Charset.forName(Optional.of(charsetName).orElse(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET.name())),
			encodeInvalid, excludeDiscouraged);
	}

	public static FlexibleCharacterEscapeHandler getInstance(final Charset charset, final boolean encodeInvalid,
		final boolean excludeDiscouraged) {
		final Charset finalCharset = Optional.of(charset).orElse(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET);
		final String key = String.format("%s:%s:%s", finalCharset.name(), encodeInvalid, excludeDiscouraged);
		return ConcurrentTools.createIfAbsent(FlexibleCharacterEscapeHandler.INSTANCES, key,
			(k) -> FlexibleCharacterEscapeHandler.getInstance(finalCharset, encodeInvalid, excludeDiscouraged));
	}

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	public static final boolean DEFAULT_ENCODE_INVALID = false;
	public static final boolean DEFAULT_EXCLUDE_DISCOURAGED = false;

	private static final String ENC_AMP = "&amp;";
	private static final String ENC_LT = "&lt;";
	private static final String ENC_GT = "&gt;";
	private static final String ENC_QUOT = "&quot;";
	private static final String ENC_ATT_QUOT = "\"";

	private final Charset charset;
	private final CharsetEncoder encoder;
	private final boolean encodeInvalid;
	private final boolean excludeDiscouraged;

	protected FlexibleCharacterEscapeHandler(Charset charset, boolean encodeInvalid, boolean excludeDiscouraged) {
		this.charset = Optional.of(charset).orElse(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET);
		this.encoder = this.charset.newEncoder();
		this.encodeInvalid = encodeInvalid;
		this.excludeDiscouraged = excludeDiscouraged;
	}

	public Marshaller unconfigureMarshaller(Marshaller m) throws PropertyException {
		Objects.requireNonNull(m, "Must provide a marshaller to unconfigure");
		for (String name : FlexibleCharacterEscapeHandler.NAMES) {
			m.setProperty(name, null);
		}
		return m;
	}

	public Marshaller configureMarshaller(Marshaller m) throws PropertyException {
		Objects.requireNonNull(m, "Must provide a marshaller to configure");
		for (String name : FlexibleCharacterEscapeHandler.NAMES) {
			m.setProperty(name, this);
		}
		return m;
	}

	public final Charset getCharset() {
		return this.charset;
	}

	public final boolean isEncodeInvalid() {
		return this.encodeInvalid;
	}

	public final boolean isExcludeDiscouraged() {
		return this.excludeDiscouraged;
	}

	protected void encodeEscaped(char c, Writer out) throws IOException {
		out.write("&#");
		out.write(Integer.toString(c));
		out.write(';');
	}

	protected boolean isDiscouragedXmlCharacter(char c) {
		if ((0x7F <= c) && (c <= 0x84)) { return true; }
		if ((0x86 <= c) && (c <= 0x9F)) { return true; }
		if ((0xFDD0 <= c) && (c <= 0xFDEF)) { return true; }
		if ((0x1FFFE <= c) && (c <= 0x1FFFF)) { return true; }
		if ((0x2FFFE <= c) && (c <= 0x2FFFF)) { return true; }
		if ((0x3FFFE <= c) && (c <= 0x3FFFF)) { return true; }
		if ((0x4FFFE <= c) && (c <= 0x4FFFF)) { return true; }
		if ((0x5FFFE <= c) && (c <= 0x5FFFF)) { return true; }
		if ((0x6FFFE <= c) && (c <= 0x6FFFF)) { return true; }
		if ((0x7FFFE <= c) && (c <= 0x7FFFF)) { return true; }
		if ((0x8FFFE <= c) && (c <= 0x8FFFF)) { return true; }
		if ((0x9FFFE <= c) && (c <= 0x9FFFF)) { return true; }
		if ((0xAFFFE <= c) && (c <= 0xAFFFF)) { return true; }
		if ((0xBFFFE <= c) && (c <= 0xBFFFF)) { return true; }
		if ((0xCFFFE <= c) && (c <= 0xCFFFF)) { return true; }
		if ((0xDFFFE <= c) && (c <= 0xDFFFF)) { return true; }
		if ((0xEFFFE <= c) && (c <= 0xEFFFF)) { return true; }
		if ((0xFFFFE <= c) && (c <= 0xFFFFF)) { return true; }
		if ((0x10FFFE <= c) && (c <= 0x10FFFF)) { return true; }
		return false;
	}

	protected boolean isInvalidXmlCharacter(char c) {
		// Per documentation at https://www.w3.org/TR/REC-xml/#NT-Char, the following are the
		// only allowed characters in XML:
		switch (c) {
			case 0x09:
			case 0x0A:
			case 0x0D:
				return false;
			default:
				break;
		}

		if ((0x20 <= c) && (c <= 0xD7FF)) { return false; }
		if ((0xE000 <= c) && (c <= 0xFFFD)) { return false; }

		if ((0x10000 <= c) && (c <= 0x10FFFF)) {
			// If we're not excluding discouraged characters, we just accept
			// whatever we got. Otherwise, we check to see if this is a character
			// that we need to exclude...
			return this.excludeDiscouraged && isDiscouragedXmlCharacter(c);
		}

		return false;
	}

	protected void encode(char c, boolean attributeValue, Writer out) throws IOException {

		// First off, the easy gimmes...
		switch (c) {
			case '&':
				out.write(FlexibleCharacterEscapeHandler.ENC_AMP);
				return;

			case '<':
				out.write(FlexibleCharacterEscapeHandler.ENC_LT);
				return;

			case '>':
				out.write(FlexibleCharacterEscapeHandler.ENC_GT);
				return;

			case '\"':
				out.write(attributeValue ? FlexibleCharacterEscapeHandler.ENC_QUOT
					: FlexibleCharacterEscapeHandler.ENC_ATT_QUOT);
				return;

			default:
				// If it's not a gimme, then keep plodding along
				break;
		}

		// If the character is allowed to be spat out verbatim, then
		// let the rest of the code do its thing. If we're not encoding
		// all characters, then "funky" characters will not be handled here
		final boolean valid = !isInvalidXmlCharacter(c);
		if (valid || this.encodeInvalid) {
			// Only valid characters are checked to see if they can
			// be encoded directly. Invalid characters all get escape-encoded
			if (valid && this.encoder.canEncode(c)) {
				out.write(c);
			} else {
				encodeEscaped(c, out);
			}
		}
	}

	@Override
	public final void escape(char[] ch, int start, int length, boolean isAttributeValue, Writer out)
		throws IOException {
		final int limit = (start + length);
		for (int i = start; i < limit; i++) {
			encode(ch[i], isAttributeValue, out);
		}
	}
}