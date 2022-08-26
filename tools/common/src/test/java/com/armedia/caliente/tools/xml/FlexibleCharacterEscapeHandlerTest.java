package com.armedia.caliente.tools.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.time.Instant;

import javax.xml.bind.Marshaller;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FlexibleCharacterEscapeHandlerTest {

	@Test
	public void testGetInstance() {
		FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance();
		Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
		Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_ENCODE_INVALID, h1.isEncodeInvalid());
		Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED, h1.isExcludeDiscouraged());
	}

	@Test
	public void testGetInstanceBoolean() {
		boolean[] arr = {
			true, false
		};
		for (boolean encodeInvalid : arr) {
			FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance(encodeInvalid);
			Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
			Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
			Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED,
				h1.isExcludeDiscouraged());
		}
	}

	@Test
	public void testGetInstanceBooleanBoolean() {
		boolean[] arr = {
			true, false
		};
		for (boolean encodeInvalid : arr) {
			for (boolean excludeDiscouraged : arr) {
				FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance(encodeInvalid,
					excludeDiscouraged);
				Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
				Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
				Assertions.assertEquals(excludeDiscouraged, h1.isExcludeDiscouraged());
			}
		}
	}

	@Test
	public void testGetInstanceString() {
		Assertions.assertThrows(IllegalCharsetNameException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("BAD CHARSET NAME"));
		Assertions.assertThrows(UnsupportedCharsetException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("CHARSET_THAT_DOES_NOT_EXIST"));

		for (String s : Charset.availableCharsets().keySet()) {
			Charset cs = Charset.forName(s);
			FlexibleCharacterEscapeHandler h1 = null;
			try {
				h1 = FlexibleCharacterEscapeHandler.getInstance(s);
			} catch (UnsupportedOperationException e) {
				// this charset doesn't support encoding ... move on
				continue;
			}
			Assertions.assertNotNull(h1);
			FlexibleCharacterEscapeHandler h2 = null;
			try {
				h2 = FlexibleCharacterEscapeHandler.getInstance(s);
			} catch (UnsupportedOperationException e) {
				// this charset doesn't support encoding ... move on
				continue;
			}
			Assertions.assertSame(h1, h2);
			Assertions.assertSame(cs, h1.getCharset());
			Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_ENCODE_INVALID, h1.isEncodeInvalid());
			Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED,
				h1.isExcludeDiscouraged());
		}

		FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance((String) null);
		Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
		Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_ENCODE_INVALID, h1.isEncodeInvalid());
		Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED, h1.isExcludeDiscouraged());
	}

	@Test
	public void testGetInstanceCharset() {
		for (String s : Charset.availableCharsets().keySet()) {
			Charset cs = Charset.forName(s);
			FlexibleCharacterEscapeHandler h1 = null;
			try {
				h1 = FlexibleCharacterEscapeHandler.getInstance(cs);
			} catch (UnsupportedOperationException e) {
				// this charset doesn't support encoding ... move on
				continue;
			}
			Assertions.assertNotNull(h1);
			FlexibleCharacterEscapeHandler h2 = null;
			try {
				h2 = FlexibleCharacterEscapeHandler.getInstance(cs);
			} catch (UnsupportedOperationException e) {
				// this charset doesn't support encoding ... move on
				continue;
			}
			Assertions.assertSame(h1, h2);
			Assertions.assertSame(cs, h1.getCharset());
			Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_ENCODE_INVALID, h1.isEncodeInvalid());
			Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED,
				h1.isExcludeDiscouraged());
		}

		FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance((Charset) null);
		Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
		Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_ENCODE_INVALID, h1.isEncodeInvalid());
		Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED, h1.isExcludeDiscouraged());
	}

	@Test
	public void testGetInstanceStringBoolean() {
		Assertions.assertThrows(IllegalCharsetNameException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("BAD CHARSET NAME", true));
		Assertions.assertThrows(IllegalCharsetNameException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("BAD CHARSET NAME", false));
		Assertions.assertThrows(UnsupportedCharsetException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("CHARSET_THAT_DOES_NOT_EXIST", true));
		Assertions.assertThrows(UnsupportedCharsetException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("CHARSET_THAT_DOES_NOT_EXIST", false));

		boolean[] arr = {
			true, false
		};
		for (boolean encodeInvalid : arr) {
			for (String s : Charset.availableCharsets().keySet()) {
				Charset cs = Charset.forName(s);
				FlexibleCharacterEscapeHandler h1 = null;
				try {
					h1 = FlexibleCharacterEscapeHandler.getInstance(s, encodeInvalid);
				} catch (UnsupportedOperationException e) {
					// this charset doesn't support encoding ... move on
					continue;
				}
				Assertions.assertNotNull(h1);
				FlexibleCharacterEscapeHandler h2 = null;
				try {
					h2 = FlexibleCharacterEscapeHandler.getInstance(s, encodeInvalid);
				} catch (UnsupportedOperationException e) {
					// this charset doesn't support encoding ... move on
					continue;
				}
				Assertions.assertSame(h1, h2);
				Assertions.assertSame(cs, h1.getCharset());
				Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
				Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED,
					h1.isExcludeDiscouraged());
			}

			FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance((String) null,
				encodeInvalid);
			Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
			Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
			Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED,
				h1.isExcludeDiscouraged());
		}
	}

	@Test
	public void testGetInstanceCharsetBoolean() {
		boolean[] arr = {
			true, false
		};
		for (boolean encodeInvalid : arr) {
			for (String s : Charset.availableCharsets().keySet()) {
				Charset cs = Charset.forName(s);
				FlexibleCharacterEscapeHandler h1 = null;
				try {
					h1 = FlexibleCharacterEscapeHandler.getInstance(cs, encodeInvalid);
				} catch (UnsupportedOperationException e) {
					// this charset doesn't support encoding ... move on
					continue;
				}
				Assertions.assertNotNull(h1);
				FlexibleCharacterEscapeHandler h2 = null;
				try {
					h2 = FlexibleCharacterEscapeHandler.getInstance(cs, encodeInvalid);
				} catch (UnsupportedOperationException e) {
					// this charset doesn't support encoding ... move on
					continue;
				}
				Assertions.assertSame(h1, h2);
				Assertions.assertSame(cs, h1.getCharset());
				Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
				Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED,
					h1.isExcludeDiscouraged());
			}

			FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance((Charset) null,
				encodeInvalid);
			Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
			Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
			Assertions.assertEquals(FlexibleCharacterEscapeHandler.DEFAULT_EXCLUDE_DISCOURAGED,
				h1.isExcludeDiscouraged());
		}
	}

	@Test
	public void testGetInstanceStringBooleanBoolean() {
		Assertions.assertThrows(IllegalCharsetNameException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("BAD CHARSET NAME", true, true));
		Assertions.assertThrows(IllegalCharsetNameException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("BAD CHARSET NAME", false, true));
		Assertions.assertThrows(IllegalCharsetNameException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("BAD CHARSET NAME", true, false));
		Assertions.assertThrows(IllegalCharsetNameException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("BAD CHARSET NAME", false, false));
		Assertions.assertThrows(UnsupportedCharsetException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("CHARSET_THAT_DOES_NOT_EXIST", true, true));
		Assertions.assertThrows(UnsupportedCharsetException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("CHARSET_THAT_DOES_NOT_EXIST", false, true));
		Assertions.assertThrows(UnsupportedCharsetException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("CHARSET_THAT_DOES_NOT_EXIST", true, false));
		Assertions.assertThrows(UnsupportedCharsetException.class,
			() -> FlexibleCharacterEscapeHandler.getInstance("CHARSET_THAT_DOES_NOT_EXIST", false, false));

		boolean[] arr = {
			true, false
		};
		for (boolean encodeInvalid : arr) {
			for (boolean excludeDiscouraged : arr) {
				for (String s : Charset.availableCharsets().keySet()) {
					Charset cs = Charset.forName(s);
					FlexibleCharacterEscapeHandler h1 = null;
					try {
						h1 = FlexibleCharacterEscapeHandler.getInstance(s, encodeInvalid, excludeDiscouraged);
					} catch (UnsupportedOperationException e) {
						// this charset doesn't support encoding ... move on
						continue;
					}
					Assertions.assertNotNull(h1);
					FlexibleCharacterEscapeHandler h2 = null;
					try {
						h2 = FlexibleCharacterEscapeHandler.getInstance(s, encodeInvalid, excludeDiscouraged);
					} catch (UnsupportedOperationException e) {
						// this charset doesn't support encoding ... move on
						continue;
					}
					Assertions.assertSame(h1, h2);
					Assertions.assertSame(cs, h1.getCharset());
					Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
					Assertions.assertEquals(excludeDiscouraged, h1.isExcludeDiscouraged());
				}

				FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance((String) null,
					encodeInvalid, excludeDiscouraged);
				Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
				Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
				Assertions.assertEquals(excludeDiscouraged, h1.isExcludeDiscouraged());
			}
		}
	}

	@Test
	public void testGetInstanceCharsetBooleanBoolean() {
		boolean[] arr = {
			true, false
		};
		for (boolean encodeInvalid : arr) {
			for (boolean excludeDiscouraged : arr) {
				for (String s : Charset.availableCharsets().keySet()) {
					Charset cs = Charset.forName(s);
					FlexibleCharacterEscapeHandler h1 = null;
					try {
						h1 = FlexibleCharacterEscapeHandler.getInstance(cs, encodeInvalid, excludeDiscouraged);
					} catch (UnsupportedOperationException e) {
						// this charset doesn't support encoding ... move on
						continue;
					}
					Assertions.assertNotNull(h1);
					FlexibleCharacterEscapeHandler h2 = null;
					try {
						h2 = FlexibleCharacterEscapeHandler.getInstance(cs, encodeInvalid, excludeDiscouraged);
					} catch (UnsupportedOperationException e) {
						// this charset doesn't support encoding ... move on
						continue;
					}
					Assertions.assertSame(h1, h2);
					Assertions.assertSame(cs, h1.getCharset());
					Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
					Assertions.assertEquals(excludeDiscouraged, h1.isExcludeDiscouraged());
				}

				FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance((Charset) null,
					encodeInvalid, excludeDiscouraged);
				Assertions.assertSame(FlexibleCharacterEscapeHandler.DEFAULT_CHARSET, h1.getCharset());
				Assertions.assertEquals(encodeInvalid, h1.isEncodeInvalid());
				Assertions.assertEquals(excludeDiscouraged, h1.isExcludeDiscouraged());
			}
		}
	}

	@Test
	public void testConfigureMarshaller() throws Exception {
		Marshaller m = EasyMock.createStrictMock(Marshaller.class);
		FlexibleCharacterEscapeHandler h1 = FlexibleCharacterEscapeHandler.getInstance(false);
		EasyMock.reset(m);
		for (String s : FlexibleCharacterEscapeHandler.NAMES) {
			m.setProperty(EasyMock.eq(s), EasyMock.same(h1));
		}
		EasyMock.replay(m);
		h1.configure(m);
		EasyMock.verify(m);

		FlexibleCharacterEscapeHandler h2 = FlexibleCharacterEscapeHandler.getInstance(true);
		EasyMock.reset(m);
		for (String s : FlexibleCharacterEscapeHandler.NAMES) {
			m.setProperty(EasyMock.eq(s), EasyMock.same(h2));
		}
		EasyMock.replay(m);
		FlexibleCharacterEscapeHandler.configure(m, h2);
		EasyMock.verify(m);
	}

	@Test
	public void testUnconfigureMarshaller() throws Exception {
		Marshaller m = EasyMock.createStrictMock(Marshaller.class);
		EasyMock.reset(m);
		for (String s : FlexibleCharacterEscapeHandler.NAMES) {
			m.setProperty(EasyMock.eq(s), EasyMock.eq(null));
		}
		EasyMock.replay(m);
		FlexibleCharacterEscapeHandler.unconfigure(m);
		EasyMock.verify(m);
	}

	@Test
	public void testEncodeEscaped() throws Exception {
		FlexibleCharacterEscapeHandler h = FlexibleCharacterEscapeHandler.getInstance();
		StringBuilder sb = new StringBuilder();
		for (long l = 0; l < Character.MAX_VALUE; l++) {
			final char c = (char) l;
			sb.setLength(0);
			sb.append("&#").append(Long.toString(l)).append(";");
			StringWriter sw = new StringWriter(16);
			h.encodeEscaped(c, sw);
			Assertions.assertEquals(sb.toString(), sw.toString());
		}
	}

	@Test
	public void testIsDiscouragedXmlCharacter() {
		FlexibleCharacterEscapeHandler h = FlexibleCharacterEscapeHandler.getInstance();
		for (long l = 0x00; l < Character.MAX_VALUE; l++) {
			final char c = (char) l;
			final boolean discouraged = false //
				|| ((0x7F <= c) && (c <= 0x84)) //
				|| ((0x86 <= c) && (c <= 0x9F)) //
				|| ((0xFDD0 <= c) && (c <= 0xFDEF)) //
				|| ((0x1FFFE <= c) && (c <= 0x1FFFF)) //
				|| ((0x2FFFE <= c) && (c <= 0x2FFFF)) //
				|| ((0x3FFFE <= c) && (c <= 0x3FFFF)) //
				|| ((0x4FFFE <= c) && (c <= 0x4FFFF)) //
				|| ((0x5FFFE <= c) && (c <= 0x5FFFF)) //
				|| ((0x6FFFE <= c) && (c <= 0x6FFFF)) //
				|| ((0x7FFFE <= c) && (c <= 0x7FFFF)) //
				|| ((0x8FFFE <= c) && (c <= 0x8FFFF)) //
				|| ((0x9FFFE <= c) && (c <= 0x9FFFF)) //
				|| ((0xAFFFE <= c) && (c <= 0xAFFFF)) //
				|| ((0xBFFFE <= c) && (c <= 0xBFFFF)) //
				|| ((0xCFFFE <= c) && (c <= 0xCFFFF)) //
				|| ((0xDFFFE <= c) && (c <= 0xDFFFF)) //
				|| ((0xEFFFE <= c) && (c <= 0xEFFFF)) //
				|| ((0xFFFFE <= c) && (c <= 0xFFFFF)) //
				|| ((0x10FFFE <= c) && (c <= 0x10FFFF)) //
			;

			Assertions.assertEquals(discouraged, h.isDiscouragedXmlCharacter(c));
		}
	}

	@Test
	public void testIsInvalidXmlCharacter() {
		boolean[] arr = {
			true, false
		};
		for (boolean excludeDiscouraged : arr) {
			FlexibleCharacterEscapeHandler h = FlexibleCharacterEscapeHandler.getInstance(true, excludeDiscouraged);
			for (long l = 0x00; l < Character.MAX_VALUE; l++) {
				final char c = (char) l;
				final boolean knownValid = false //
					|| (0x09 == c) //
					|| (0x0A == c) //
					|| (0x0D == c) //
					|| ((0x20 <= c) && (c <= 0xD7FF)) //
					|| ((0xE000 <= c) && (c <= 0xFFFD)) //
				;
				final boolean maybeValid = ((0x10000 <= c) && (c <= 0x10FFFF));

				final boolean valid = knownValid //
					|| (maybeValid && (!excludeDiscouraged || !h.isDiscouragedXmlCharacter(c))) //
				;

				Assertions.assertNotEquals(valid, h.isInvalidXmlCharacter(c));
			}
		}
	}

	@Test
	public void testEncode() throws IOException {
		boolean[] arr = {
			true, false
		};
		CharsetEncoder enc = null;
		for (boolean encodeInvalid : arr) {
			for (boolean excludeDiscouraged : arr) {
				for (boolean attributeValue : arr) {
					FlexibleCharacterEscapeHandler h = FlexibleCharacterEscapeHandler.getInstance(encodeInvalid,
						excludeDiscouraged);
					if (enc == null) {
						enc = h.getCharset().newEncoder();
					}
					for (long l = 0x00; l < Character.MAX_VALUE; l++) {
						final char c = (char) l;
						String result = StringUtils.EMPTY;
						switch (c) {
							case '&':
								result = "&amp;";
								break;
							case '<':
								result = "&lt;";
								break;
							case '>':
								result = "&gt;";
								break;
							case '"':
								result = (attributeValue ? "&quot;" : "\"");
								break;
							default:
								boolean valid = !h.isInvalidXmlCharacter(c);
								if (valid || encodeInvalid) {
									if (valid && enc.canEncode(c)) {
										result = String.valueOf(c);
									} else {
										result = "&#" + Integer.toString(c) + ";";
									}
								}
								break;
						}

						StringWriter sw = new StringWriter();
						h.encode(c, attributeValue, sw);
						Assertions.assertEquals(result, sw.toString());
					}
				}
			}
		}
	}

	@Test
	public void testEscape() throws IOException {
		boolean[] arr = {
			true, false
		};
		char[] chars = new char[Character.MAX_VALUE];
		for (int i = 0x00; i < Character.MAX_VALUE; i++) {
			chars[i] = (char) i;
		}

		StringBuilder encoded = new StringBuilder();

		for (boolean encodeInvalid : arr) {
			for (boolean excludeDiscouraged : arr) {
				for (boolean attributeValue : arr) {
					FlexibleCharacterEscapeHandler h = FlexibleCharacterEscapeHandler.getInstance(encodeInvalid,
						excludeDiscouraged);

					// Test 0 length
					for (int s = 0; s < chars.length; s++) {
						StringWriter sw = new StringWriter();
						h.escape(chars, s, 0, attributeValue, sw);
						Assertions.assertEquals(StringUtils.EMPTY, sw.toString());
					}

					// Test all lengths
					long i = 0;
					Instant last = Instant.now();
					for (int s = 0; s < chars.length; s++) {
						final int maxLength = (chars.length - s);
						for (int l = 0; l < maxLength; l++) {
							encoded.setLength(0);

							for (int o = s; o < (s + l); o++) {
								StringWriter sw = new StringWriter();
								h.encode(chars[o], attributeValue, sw);
								encoded.append(sw.toString());
							}

							StringWriter sw = new StringWriter();
							h.escape(chars, s, l, attributeValue, sw);
							Assertions.assertEquals(encoded.toString(), sw.toString(),
								String.format("Offsets: %d:%d", s, l));

							if (Math.floorMod(i, 10000) == 0) {
								Instant next = Instant.now();
								System.out.printf("%s: %d (%d:%d)%n", Duration.between(last, next), i, s, l);
								last = next;
							}
							i++;
						}
					}
				}
			}
		}
	}
}