package com.armedia.caliente.filenamemapper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.caliente.cli.filenamemapper.FilenameMapper.Fixer;
import com.armedia.commons.utilities.Tools;

public class FilenameMapperTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testWIN() {
		Object[][] data = {
			{
				null, IllegalArgumentException.class
			}, {
				"    This is a legal filename under all Windows Rules 993481934 134 9             91234 91234 91243 .doc"
			}, {
				".illegal.filename.", ".illegal.filename_"
			}, {
				".illegal.filename ", ".illegal.filename_"
			}, {
				".illegal/filename", ".illegal_filename"
			}, {
				".badchar-\".doc", ".badchar-_.doc"
			}, {
				".badchar-*.doc", ".badchar-_.doc"
			}, {
				".badchar-\\.doc", ".badchar-_.doc"
			}, {
				".badchar->.doc", ".badchar-_.doc"
			}, {
				".badchar-<.doc", ".badchar-_.doc"
			}, {
				".badchar-?.doc", ".badchar-_.doc"
			}, {
				".badchar-/.doc", ".badchar-_.doc"
			}, {
				".badchar-:.doc", ".badchar-_.doc"
			}, {
				".badchar-|.doc", ".badchar-_.doc"
			}, {
				".badchar-\u0000.doc", ".badchar-_.doc"
			}, {
				"CON", "_CON"
			}, {
				"PRN", "_PRN"
			}, {
				"AUX", "_AUX"
			}, {
				"NUL", "_NUL"
			}, {
				"COM1", "_COM1"
			}, {
				"COM2", "_COM2"
			}, {
				"COM3", "_COM3"
			}, {
				"COM4", "_COM4"
			}, {
				"COM5", "_COM5"
			}, {
				"COM6", "_COM6"
			}, {
				"COM7", "_COM7"
			}, {
				"COM8", "_COM8"
			}, {
				"COM9", "_COM9"
			}, {
				"LPT1", "_LPT1"
			}, {
				"LPT2", "_LPT2"
			}, {
				"LPT3", "_LPT3"
			}, {
				"LPT4", "_LPT4"
			}, {
				"LPT5", "_LPT5"
			}, {
				"LPT6", "_LPT6"
			}, {
				"LPT7", "_LPT7"
			}, {
				"LPT8", "_LPT8"
			}, {
				"LPT9", "_LPT9"
			}, {
				"con", "_con"
			}, {
				"prn", "_prn"
			}, {
				"aux", "_aux"
			}, {
				"nul", "_nul"
			}, {
				"com1", "_com1"
			}, {
				"com2", "_com2"
			}, {
				"com3", "_com3"
			}, {
				"com4", "_com4"
			}, {
				"com5", "_com5"
			}, {
				"com6", "_com6"
			}, {
				"com7", "_com7"
			}, {
				"com8", "_com8"
			}, {
				"com9", "_com9"
			}, {
				"lpt1", "_lpt1"
			}, {
				"lpt2", "_lpt2"
			}, {
				"lpt3", "_lpt3"
			}, {
				"lpt4", "_lpt4"
			}, {
				"lpt5", "_lpt5"
			}, {
				"lpt6", "_lpt6"
			}, {
				"lpt7", "_lpt7"
			}, {
				"lpt8", "_lpt8"
			}, {
				"lpt9", "_lpt9"
			}, {
				"superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandis/illegalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandisil_legalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l.illegal.filename.doc",
				"superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandis_illegalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandisil_legalinwindows ealskjf lfksajlsdfjlas.doc"
			}, {
				"superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandis/illegalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandisil_legalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l.illegal.filename.doc ",
				"superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandis_illegalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandisil_legalinwindows ealskjf lfksajlsdfjla.doc_"
			}
		};

		boolean fixLength = true;
		char fixChar = '_';

		Fixer f = Fixer.WIN;
		for (Object[] o : data) {
			// Bad data...
			final String key = Tools.toString(o[0]);
			final String msg = String.format("%s: [%s]", f.name(), key);
			Object value = key;
			if (o.length >= 2) {
				value = o[1];
			}

			if (Class.class.isInstance(value)) {
				Class<?> expected = Class.class.cast(value);
				if (expected.isAssignableFrom(Throwable.class)) {
					// The invocation must result in the given exception
					try {
						f.fixName(key, fixChar, fixLength);
					} catch (Throwable t) {
						Assert.assertTrue(msg, expected.isInstance(t));
					}
				} else {
					// WTF is it?
				}
			} else if (String.class.isInstance(value)) {
				Assert.assertEquals(msg, value, f.fixName(key, fixChar, fixLength));
			} else if (value == null) {
				Assert.assertNull(msg, f.fixName(key, fixChar, fixLength));
			} else {
				/// WTF is it??
			}
		}
	}

	@Test
	public void testUNIX() {
		Object[][] data = {
			{
				null, IllegalArgumentException.class
			}, {
				"    This is a legal filename under all Unix Rules 993481934 134 9             91234 91234 91243 .doc           "
			}, {
				".badchar-/.doc", ".badchar-_.doc"
			}, {
				".badchar-\u0000.doc", ".badchar-_.doc"
			}, {
				"CON",
			}, {
				"PRN",
			}, {
				"AUX",
			}, {
				"NUL",
			}, {
				"COM1",
			}, {
				"COM2",
			}, {
				"COM3",
			}, {
				"COM4",
			}, {
				"COM5",
			}, {
				"COM6",
			}, {
				"COM7",
			}, {
				"COM8",
			}, {
				"COM9",
			}, {
				"LPT1",
			}, {
				"LPT2",
			}, {
				"LPT3",
			}, {
				"LPT4",
			}, {
				"LPT5",
			}, {
				"LPT6",
			}, {
				"LPT7",
			}, {
				"LPT8",
			}, {
				"LPT9",
			}, {
				"con",
			}, {
				"prn",
			}, {
				"aux",
			}, {
				"nul",
			}, {
				"com1",
			}, {
				"com2",
			}, {
				"com3",
			}, {
				"com4",
			}, {
				"com5",
			}, {
				"com6",
			}, {
				"com7",
			}, {
				"com8",
			}, {
				"com9",
			}, {
				"lpt1",
			}, {
				"lpt2",
			}, {
				"lpt3",
			}, {
				"lpt4",
			}, {
				"lpt5",
			}, {
				"lpt6",
			}, {
				"lpt7",
			}, {
				"lpt8",
			}, {
				"lpt9",
			}, {
				"superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandis/illegalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandisil_legalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l.illegal.filename.doc ",
				"superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandis_illegalinwindows ealskjf lfksajlsdfjlasdfj lasjf lajsf ljasdfl jslfdj l superlongfilenamethatexceedstwohundredandfiftyfivecharacterslongandisil_legalinwindows ealskjf lfksajlsdfjlasdfj "
			}
		};

		boolean fixLength = true;
		char fixChar = '_';

		Fixer f = Fixer.UNIX;
		for (Object[] o : data) {
			// Bad data...
			final String key = Tools.toString(o[0]);
			final String msg = String.format("%s: [%s]", f.name(), key);
			Object value = key;
			if (o.length >= 2) {
				value = o[1];
			}

			if (Class.class.isInstance(value)) {
				Class<?> expected = Class.class.cast(value);
				if (expected.isAssignableFrom(Throwable.class)) {
					// The invocation must result in the given exception
					try {
						f.fixName(key, fixChar, fixLength);
					} catch (Throwable t) {
						Assert.assertTrue(msg, expected.isInstance(t));
					}
				} else {
					// WTF is it?
				}
			} else if (String.class.isInstance(value)) {
				Assert.assertEquals(msg, value, f.fixName(key, fixChar, fixLength));
			} else if (value == null) {
				Assert.assertNull(msg, f.fixName(key, fixChar, fixLength));
			} else {
				/// WTF is it??
			}
		}
	}
}