package com.armedia.caliente.tools.xml;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

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
	public void testFlexibleCharacterEscapeHandler() {
	}

	@Test
	public void testUnconfigureMarshaller() {
	}

	@Test
	public void testConfigureMarshaller() {
	}

	@Test
	public void testGetCharset() {
	}

	@Test
	public void testIsEncodeInvalid() {
	}

	@Test
	public void testIsExcludeDiscouraged() {
	}

	@Test
	public void testEncodeEscaped() {
	}

	@Test
	public void testIsDiscouragedXmlCharacter() {
	}

	@Test
	public void testIsInvalidXmlCharacter() {
	}

	@Test
	public void testEncode() {
	}

	@Test
	public void testEscape() {
	}
}