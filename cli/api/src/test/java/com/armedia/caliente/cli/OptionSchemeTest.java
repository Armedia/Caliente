package com.armedia.caliente.cli;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.cli.exception.CommandLineSyntaxException;
import com.armedia.caliente.cli.exception.DuplicateOptionException;
import com.armedia.caliente.cli.exception.HelpRequestedException;
import com.armedia.caliente.cli.token.Token;

public class OptionSchemeTest {

	@Test
	public void testConstructor() {
		OptionScheme cl = new OptionScheme("test");
		Assertions.assertNotNull(cl);
	}

	@Test
	public void testDefine() throws Exception {
		OptionScheme cl = new OptionScheme("test");
		Assertions.assertNotNull(cl);

		OptionImpl def = null;

		try {
			cl.add(def);
			Assertions.fail("Did not fail with a null parameter");
		} catch (NullPointerException e) {
			// All is well
		}

		def = new OptionImpl();
		try {
			cl.add(def);
			Assertions.fail("Did not fail with no short or long options");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		// Only test for the ASCII extended set
		String[] longOpts = {
			null, "%s-longOpt"
		};
		final Charset charset = Charset.forName("US-ASCII");
		for (String longOpt : longOpts) {
			for (int i = 0; i < 255; i++) {
				ByteBuffer bb = ByteBuffer.allocate(4);
				bb.putInt(i);
				String s = new String(bb.array(), charset).trim();
				if (s.length() != 1) {
					continue;
				}
				char c = s.charAt(0);

				def = new OptionImpl();
				if (Character.isLetterOrDigit(c) || (c == '?') || (c == '$')) {
					def.setShortOpt(c);
				} else {
					try {
						def.setShortOpt(c);
						Assertions.fail(String.format("Did not fail with illegal character [%s]", c));
					} catch (IllegalArgumentException e) {
						// All is well
					}
				}

				if (longOpt != null) {
					longOpt = String.format(longOpt, c);
					if (Option.VALID_LONG.matcher(longOpt).matches()) {
						def.setLongOpt(longOpt);
					} else {
						try {
							def.setLongOpt(longOpt);
							Assertions.fail(String.format("Did not fail with illegal character [%s]", c));
						} catch (IllegalArgumentException e) {
							// All is well
						}
					}
				}
				try {
					cl.add(def);
					Option p = null;
					if (longOpt != null) {
						p = cl.getOption(longOpt);
					} else {
						p = cl.getOption(c);
					}
					if (!Character.isLetterOrDigit(c) && (c != '?') && (c != '$')) {
						Assertions.fail(String.format("Did not fail with illegal character [%s]", c));
					}
					Assertions.assertTrue(Option.isIdentical(def, p));
				} catch (IllegalArgumentException e) {
					if (Character.isLetterOrDigit(c)) {
						Assertions.fail(String.format("Failed with legal character [%s]", c));
					}
				} catch (DuplicateOptionException e) {
					if (c != '?') {
						if (longOpt == null) {
							Assertions.fail(String.format(
								"Duplicate exception caught when no duplicate was in play (%s) existing = [%s] incoming = [%s]",
								c, e.getExisting().getKey(), e.getIncoming().getKey()));
						}
					}
				}
			}
		}

		def = new OptionImpl();
		try {
			def.setLongOpt("");
			Assertions.fail("Did not fail with an empty long option");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			def.setLongOpt("a");
			Assertions.fail("Did not fail with a length-1 long option");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			def.setLongOpt("  ");
			Assertions.fail("Did not fail with a long option with spaces");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		def.setLongOpt("ab");
		cl.add(def);
		Option p = cl.getOption(def.getLongOpt());
		Assertions.assertEquals(p, p);
		Assertions.assertNotEquals(p, null);
		Assertions.assertNotEquals(p, "");
		// Assertions.assertSame(cl, p.getParameterSchemeValues());
		Assertions.assertTrue(Option.isIdentical(def, p));

		cl.add(def);
		Option p2 = cl.getOption(def.getLongOpt());
		Assertions.assertSame(p, p2);

		def.setLongOpt("cd");
		cl.add(def);
		p2 = cl.getOption(def.getLongOpt());
		Assertions.assertSame(p, p2);
		Assertions.assertTrue(Option.isIdentical(def, p2));
		Assertions.assertEquals(p, p2);

		def.setShortOpt('x');
		try {
			cl.add(def);
			Assertions.fail(String.format("Did not fail with a duplicate option"));
		} catch (DuplicateOptionException e) {
			// All is well
		}
	}

	@Test
	public void testIterator() throws Exception {
		OptionScheme cl = new OptionScheme("test");
		Assertions.assertNotNull(cl);

		OptionImpl def = new OptionImpl();
		Map<String, Option> m = new HashMap<>();
		for (int i = 0; i < 100; i++) {
			def.setLongOpt(String.format("long-%04x", i));
			cl.add(def);
			m.put(def.getLongOpt(), def);
		}

		for (Option p : cl.getOptions()) {
			final String longOpt = p.getLongOpt();
			if ("help".equals(longOpt)) {
				// We're OK here...
				continue;
			}
			Assertions.assertTrue(m.containsKey(longOpt), String.format("Unexpected long option [%s]", longOpt));
		}
	}

	@Test
	public void testHelp() throws Exception {
		OptionParser parser = new OptionParser();
		OptionParseResult result = null;

		OptionScheme cl = new OptionScheme("test");

		Option help = new OptionImpl() //
			.setShortOpt('?') //
			.setLongOpt("help") //
		;

		Assertions.assertNotNull(help);
		Assertions.assertNotNull(help.getKey());

		String[] args = {
			"-a", "true", //
			"-b", "false", //
			"-c", //
		};

		cl.add( //
			new OptionImpl() //
				.setMinArguments(1) //
				.setMaxArguments(1) //
				.setShortOpt('a') //
		);

		cl.add( //
			new OptionImpl() //
				.setMinArguments(1) //
				.setMaxArguments(1) //
				.setShortOpt('b') //
		);

		cl.add( //
			new OptionImpl() //
				.setShortOpt('c') //
		);

		result = parser.parse(cl, null, args);
		Assertions.assertNotNull(result);

		args = new String[] {
			"-a", "true", //
			"-c", "false", //
			"-b" //
		};

		try {
			result = parser.parse(help, cl, null, args);
			Assertions.fail("Did not raise a command line syntax exception when the syntax was incorrect");
		} catch (CommandLineSyntaxException e) {
			// All is well
		}

		args = new String[] {
			"-a", "true", //
			"-c", "false", //
			"-?", //
		};

		try {
			result = parser.parse(help, cl, null, args);
			Assertions.fail("Did not raise a help exception when help was requested");
		} catch (HelpRequestedException e) {
			// All is well
		}
	}

	@Test
	public void testExtension() throws Exception {
		OptionParser parser = new OptionParser();
		OptionParseResult result = null;

		OptionScheme cl = new OptionScheme("test");

		Option help = new OptionImpl() //
			.setShortOpt('?') //
			.setLongOpt("help") //
		;

		Assertions.assertNotNull(help);
		Assertions.assertNotNull(help.getKey());

		String[] args = {
			"-a", "true", //
			"-b", "false", //
			"-c", //
			"more", "positional", "parameters" //
		};

		cl.add( //
			new OptionImpl() //
				.setMinArguments(1) //
				.setMaxArguments(1) //
				.setShortOpt('a') //
		);

		OptionSchemeExtensionSupport extender = new OptionSchemeExtensionSupport() {

			@Override
			public void extendScheme(int currentNumber, OptionValues baseValues, String currentCommand,
				OptionValues commandValues, Token currentToken, OptionSchemeExtender extender) {
				switch (currentNumber) {
					case 1:
						// Add -b
						extender.addGroup(new OptionGroupImpl("Letter-B").add(new OptionImpl() //
							.setMinArguments(1) //
							.setMaxArguments(1) //
							.setShortOpt('b') //
						));
						return;
					case 2:
						// Add -c
						extender.addGroup(new OptionGroupImpl("Letter-C").add(new OptionImpl() //
							.setShortOpt('c') //
						));
						return;
				}
			}
		};

		result = parser.parse(cl, extender, args);
		Assertions.assertNotNull(result);

		args = new String[] {
			"-a", "true", //
			"-c", "false", //
			"-b" //
		};

		try {
			result = parser.parse(help, cl, extender, args);
			Assertions.fail("Did not raise a command line syntax exception when the syntax was incorrect");
		} catch (CommandLineSyntaxException e) {
			// All is well
		}

		args = new String[] {
			"-a", "true", //
			"-c", "false", //
			"-?", //
		};

		try {
			result = parser.parse(help, cl, extender, args);
			Assertions.fail("Did not raise a help exception when help was requested");
		} catch (HelpRequestedException e) {
			// All is well
		}
	}

	/*
	@Test
	public void testGetBoolean() throws Exception {
		OptionScheme cl = new OptionScheme("test");
	
		String[] args = {
			"-a", "true", //
			"-b", "false", //
			"-c", "true,false", //
			"-d", "garbage", //
			"-f", //
		};
	
		OptionImpl def = new OptionImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.add(def);
		Option a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.add(def);
		Option b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.add(def);
		Option d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.add(def);
		Option e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.add(def);
		Option c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.add(def);
		Option f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assertions.assertTrue("a", a.getBoolean());
		Assertions.assertTrue("a-def", a.getBoolean(false));
		Assertions.assertFalse("b", b.getBoolean());
		Assertions.assertFalse("b-def", b.getBoolean(true));
		Assertions.assertEquals("a-dual", b.getBoolean(), cl.getBoolean(b));
		Assertions.assertEquals("a-dual-def", b.getBoolean(false), cl.getBoolean(b, false));
		List<Boolean> C = c.getAllBooleans();
		Assertions.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assertions.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Boolean v = Boolean.valueOf(CS.get(i));
			Assertions.assertEquals(v, C.get(i));
		}
		Assertions.assertFalse("d", d.getBoolean());
		Assertions.assertNull("e", e.getBoolean());
		Assertions.assertNull("e-all", e.getAllBooleans());
		Assertions.assertTrue("e-def-true", e.getBoolean(true));
		Assertions.assertFalse("e-def-false", e.getBoolean(false));
		Assertions.assertNull("f", f.getBoolean());
		Assertions.assertTrue("f-all", f.getAllBooleans().isEmpty());
	}
	
	@Test
	public void testGetInteger() throws Exception {
		OptionScheme cl = new OptionScheme("test");
	
		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
			"-f", //
		};
	
		OptionImpl def = new OptionImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.add(def);
		Option a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.add(def);
		Option b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.add(def);
		Option d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.add(def);
		Option e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.add(def);
		Option c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.add(def);
		Option f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assertions.assertEquals("a", Integer.valueOf(1), a.getInteger());
		Assertions.assertEquals("a-def", 1, a.getInteger(2));
		Assertions.assertEquals("a-dual", b.getInteger(), cl.getInteger(b));
		Assertions.assertEquals("a-dual-def", b.getInteger(2), cl.getInteger(b, 2));
		Assertions.assertEquals("b", Integer.valueOf(2), b.getInteger());
		Assertions.assertEquals("b-def", 2, b.getInteger(3));
		List<Integer> C = c.getAllIntegers();
		Assertions.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assertions.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Integer v = Integer.valueOf(CS.get(i));
			Assertions.assertEquals(v, C.get(i));
		}
		Assertions.assertEquals("d", Integer.valueOf(5), d.getInteger());
		Assertions.assertNull("e", e.getInteger());
		Assertions.assertNull("e-all", e.getAllIntegers());
		Assertions.assertEquals("e-def-max", Integer.MAX_VALUE, e.getInteger(Integer.MAX_VALUE));
		Assertions.assertEquals("e-def-min", Integer.MIN_VALUE, e.getInteger(Integer.MIN_VALUE));
		for (int i = 0; i < 100; i++) {
			int r = UUID.randomUUID().hashCode();
			Assertions.assertEquals("e-def", r, e.getInteger(r));
		}
		Assertions.assertNull("f", f.getInteger());
		Assertions.assertTrue("f-all", f.getAllIntegers().isEmpty());
	}
	
	@Test
	public void testGetLong() throws Exception {
		OptionScheme cl = new OptionScheme("test");
	
		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
			"-f", //
		};
	
		OptionImpl def = new OptionImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.add(def);
		Option a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.add(def);
		Option b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.add(def);
		Option d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.add(def);
		Option e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.add(def);
		Option c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.add(def);
		Option f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assertions.assertEquals("a", Long.valueOf(1), a.getLong());
		Assertions.assertEquals("a-def", 1, a.getLong(2));
		Assertions.assertEquals("b", Long.valueOf(2), b.getLong());
		Assertions.assertEquals("b-def", 2, b.getLong(3));
		Assertions.assertEquals("a-dual", b.getLong(), cl.getLong(b));
		Assertions.assertEquals("a-dual-def", b.getLong(2), cl.getLong(b, 2));
		List<Long> C = c.getAllLongs();
		Assertions.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assertions.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Long v = Long.valueOf(CS.get(i));
			Assertions.assertEquals(v, C.get(i));
		}
		Assertions.assertEquals("d", Long.valueOf(5), d.getLong());
		Assertions.assertNull("e", e.getLong());
		Assertions.assertNull("e-all", e.getAllLongs());
		Assertions.assertEquals("e-def-max", Long.MAX_VALUE, e.getLong(Long.MAX_VALUE));
		Assertions.assertEquals("e-def-min", Long.MIN_VALUE, e.getLong(Long.MIN_VALUE));
		for (int i = 0; i < 100; i++) {
			long r = UUID.randomUUID().hashCode() * UUID.randomUUID().hashCode();
			Assertions.assertEquals("e-def", r, e.getLong(r));
		}
		Assertions.assertNull("f", f.getLong());
		Assertions.assertTrue("f-all", f.getAllLongs().isEmpty());
	}
	
	@Test
	public void testGetFloat() throws Exception {
		OptionScheme cl = new OptionScheme("test");
	
		String[] args = {
			"-a", "1.1", //
			"-b", "2.2", //
			"-c", "3.3,4.4", //
			"-d", "5.5", //
			"-f", //
		};
	
		OptionImpl def = new OptionImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.add(def);
		Option a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.add(def);
		Option b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.add(def);
		Option d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.add(def);
		Option e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.add(def);
		Option c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.add(def);
		Option f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assertions.assertEquals("a", Float.valueOf(1.1f), a.getFloat());
		Assertions.assertEquals("a-def", Float.valueOf(1.1f), a.getFloat(2.2f), 0.0f);
		Assertions.assertEquals("b", Float.valueOf(2.2f), b.getFloat());
		Assertions.assertEquals("b-def", Float.valueOf(2.2f), b.getFloat(1.1f), 0.0f);
		Assertions.assertEquals("a-dual", b.getFloat(), cl.getFloat(b));
		Assertions.assertEquals("a-dual-def", b.getFloat(2), cl.getFloat(b, 2), 0.0f);
		List<Float> C = c.getAllFloats();
		Assertions.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assertions.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Float v = Float.valueOf(CS.get(i));
			Assertions.assertEquals(v, C.get(i));
		}
		Assertions.assertEquals("d", Float.valueOf(5.5f), d.getFloat());
		Assertions.assertNull("e", e.getFloat());
		Assertions.assertNull("e-all", e.getAllFloats());
		Assertions.assertEquals("e-def-max", Float.MAX_VALUE, e.getFloat(Float.MAX_VALUE), 0.0f);
		Assertions.assertEquals("e-def-min", Float.MIN_VALUE, e.getFloat(Float.MIN_VALUE), 0.0f);
		Assertions.assertEquals("e-def-nan", Float.NaN, e.getFloat(Float.NaN), 0.0f);
		Assertions.assertEquals("e-def-+inf", Float.POSITIVE_INFINITY, e.getFloat(Float.POSITIVE_INFINITY), 0.0f);
		Assertions.assertEquals("e-def--inf", Float.NEGATIVE_INFINITY, e.getFloat(Float.NEGATIVE_INFINITY), 0.0f);
		Assertions.assertNull("f", f.getFloat());
		Assertions.assertTrue("f-all", f.getAllFloats().isEmpty());
	}
	
	@Test
	public void testGetDouble() throws Exception {
		OptionScheme cl = new OptionScheme("test");
	
		String[] args = {
			"-a", "1.1", //
			"-b", "2.2", //
			"-c", "3.3,4.4", //
			"-d", "5.5", //
			"-f", //
		};
	
		OptionImpl def = new OptionImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.add(def);
		Option a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.add(def);
		Option b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.add(def);
		Option d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.add(def);
		Option e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.add(def);
		Option c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.add(def);
		Option f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assertions.assertEquals("a", Double.valueOf(1.1), a.getDouble());
		Assertions.assertEquals("a-def", Double.valueOf(1.1), a.getDouble(2.2), 0.0);
		Assertions.assertEquals("b", Double.valueOf(2.2), b.getDouble());
		Assertions.assertEquals("b-def", Double.valueOf(2.2), b.getDouble(3.3), 0.0);
		Assertions.assertEquals("a-dual", b.getDouble(), cl.getDouble(b));
		Assertions.assertEquals("a-dual-def", b.getDouble(2), cl.getDouble(b, 2), 0.0);
		List<Double> C = c.getAllDoubles();
		Assertions.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assertions.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Double v = Double.valueOf(CS.get(i));
			Assertions.assertEquals(v, C.get(i));
		}
		Assertions.assertEquals("d", Double.valueOf(5.5), d.getDouble());
		Assertions.assertNull("e", e.getDouble());
		Assertions.assertNull("e-all", e.getAllDoubles());
		Assertions.assertEquals("e-def-max", Double.MAX_VALUE, e.getDouble(Double.MAX_VALUE), 0.0);
		Assertions.assertEquals("e-def-min", Double.MIN_VALUE, e.getDouble(Double.MIN_VALUE), 0.0);
		Assertions.assertEquals("e-def-nan", Double.NaN, e.getDouble(Double.NaN), 0.0);
		Assertions.assertEquals("e-def-+inf", Double.POSITIVE_INFINITY, e.getDouble(Double.POSITIVE_INFINITY), 0.0);
		Assertions.assertEquals("e-def--inf", Double.NEGATIVE_INFINITY, e.getDouble(Double.NEGATIVE_INFINITY), 0.0);
		Assertions.assertNull("f", f.getDouble());
		Assertions.assertTrue("f-all", f.getAllDoubles().isEmpty());
	}
	
	@Test
	public void testGetString() throws Exception {
		OptionScheme cl = new OptionScheme("test");
	
		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
			"-f",
		};
	
		OptionImpl def = new OptionImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.add(def);
		Option a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.add(def);
		Option b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.add(def);
		Option d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.add(def);
		Option e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.add(def);
		Option c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.add(def);
		Option f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assertions.assertEquals("a", "1", a.getString());
		Assertions.assertEquals("a-def", "1", a.getString("x"));
		Assertions.assertEquals("b", "2", b.getString());
		Assertions.assertEquals("b-def", "2", b.getString("x"));
		Assertions.assertEquals("a-dual", b.getString(), cl.getString(b));
		Assertions.assertEquals("a-dual-def", b.getString("x"), cl.getString(b, "x"));
		List<String> C = c.getAllStrings();
		Assertions.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assertions.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Assertions.assertEquals(CS.get(i), C.get(i));
		}
		Assertions.assertEquals("d", "5", d.getString());
		Assertions.assertEquals("d", "5", d.getString("x"));
		Assertions.assertNull("e", e.getString());
		Assertions.assertNull("e-all", e.getAllStrings());
		for (int i = 0; i < 100; i++) {
			String s = UUID.randomUUID().toString();
			Assertions.assertEquals("e-def", s, e.getString(s));
		}
		Assertions.assertNull("f", f.getString());
		Assertions.assertTrue("f-all", f.getAllStrings().isEmpty());
	}
	
	@Test
	public void testIsPresent() throws Exception {
		// Short options
		{
			OptionScheme cl = new OptionScheme("test");
	
			String[] args = {
				"-a", //
				"-b", "2", //
				"-c", "3,4", //
			};
	
			OptionImpl def = new OptionImpl();
			def.setShortOpt('a');
			cl.add(def);
			Option a = cl.getParameter(def);
	
			def.setMaxValueCount(1);
			def.setShortOpt('b');
			cl.add(def);
			Option b = cl.getParameter(def);
	
			def.setMaxValueCount(-1);
			def.setShortOpt('c');
			cl.add(def);
			Option c = cl.getParameter(def);
	
			def.setShortOpt('d');
			cl.add(def);
			Option d = cl.getParameter(def);
	
			cl.parse("TEST", args);
	
			Assertions.assertTrue(a.isPresent());
			Assertions.assertEquals(a.isPresent(), cl.isPresent(a));
			Assertions.assertTrue(b.isPresent());
			Assertions.assertTrue(c.isPresent());
			Assertions.assertFalse(d.isPresent());
			Assertions.assertEquals(d.isPresent(), cl.isPresent(d));
		}
		// Long options
		{
			OptionScheme cl = new OptionScheme("test");
	
			String[] args = {
				"--long-a", //
				"--long-b", "2", //
				"--long-c", "3,4", //
			};
	
			OptionImpl def = new OptionImpl();
			def.setLongOpt("long-a");
			cl.add(def);
			Option a = cl.getParameter(def);
	
			def.setMaxValueCount(1);
			def.setLongOpt("long-b");
			cl.add(def);
			Option b = cl.getParameter(def);
	
			def.setMaxValueCount(-1);
			def.setLongOpt("long-c");
			cl.add(def);
			Option c = cl.getParameter(def);
	
			def.setLongOpt("long-d");
			cl.add(def);
			Option d = cl.getParameter(def);
	
			cl.parse("TEST", args);
	
			Assertions.assertTrue(a.isPresent());
			Assertions.assertEquals(a.isPresent(), cl.isPresent(a));
			Assertions.assertTrue(b.isPresent());
			Assertions.assertTrue(c.isPresent());
			Assertions.assertFalse(d.isPresent());
			Assertions.assertEquals(d.isPresent(), cl.isPresent(d));
		}
	}
	
	@Test
	public void testGetRemainingParameters() throws Exception {
		// With remaining
		{
			OptionScheme cl = new OptionScheme("test");
	
			String[] args = {
				"-a", "1", //
				"b", "2", //
				"c", "3,4", //
				"d", "5", //
			};
	
			OptionImpl def = new OptionImpl();
			def.setMaxValueCount(1);
			def.setShortOpt('a');
			cl.add(def);
			Option a = cl.getParameter(def);
	
			cl.parse("TEST", args);
	
			Assertions.assertEquals("a", Integer.valueOf(1), a.getInteger());
			List<String> remaining = cl.getPositionalValues();
			Assertions.assertEquals(6, remaining.size());
			for (int i = 2; i < args.length; i++) {
				Assertions.assertEquals(args[i], remaining.get(i - 2));
			}
		}
		// Without remaining
		{
			OptionScheme cl = new OptionScheme("test");
	
			String[] args = {
				"-a", "1", //
			};
	
			OptionImpl def = new OptionImpl();
			def.setMaxValueCount(1);
			def.setShortOpt('a');
			cl.add(def);
			Option a = cl.getParameter(def);
	
			cl.parse("TEST", args);
	
			Assertions.assertEquals("a", Integer.valueOf(1), a.getInteger());
			List<String> remaining = cl.getPositionalValues();
			Assertions.assertTrue(remaining.isEmpty());
		}
	}
	
	@Test
	public void testShortOptions() {
		OptionScheme cl = new OptionScheme(false);
		final Charset charset = Charset.forName("US-ASCII");
		OptionImpl def = null;
		Set<Option> shortOptions = new HashSet<>();
		for (int i = 0; i < 255; i++) {
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(i);
			String s = new String(bb.array(), charset).trim();
			if (s.length() != 1) {
				continue;
			}
			char c = s.charAt(0);
			def = new OptionImpl();
			def.setShortOpt(c);
			try {
				cl.add(def);
				Option p = cl.getParameter(def);
				shortOptions.add(p);
				Assertions.assertEquals(1, p.compareTo(null));
			} catch (IllegalArgumentException e) {
				if (Character.isLetterOrDigit(c)) {
					Assertions.fail(String.format("Failed with legal character [%s]", c));
				}
			} catch (DuplicateOptionException e) {
				Assertions.fail(String.format("Duplicate exception caught when no duplicate was in play (%s)", c));
			}
		}
	
		for (Option expected : shortOptions) {
			Option actual = cl.getParameter(expected.getShortOpt());
			Assertions.assertEquals(expected, actual);
			Assertions.assertTrue(cl.hasParameter(expected.getShortOpt()));
		}
	
		for (Option actual : cl.shortOptions()) {
			Assertions.assertNotNull(actual);
			Assertions.assertTrue(shortOptions.remove(actual));
		}
		Assertions.assertTrue(shortOptions.isEmpty());
	}
	
	@Test
	public void testLongOptions() throws Exception {
		OptionScheme cl = new OptionScheme("");
		OptionImpl def = null;
		Set<Option> longOptions = new HashSet<>();
		for (int i = 0; i < 255; i++) {
			def = new OptionImpl();
			def.setLongOpt(String.format("long-%04x", i));
			cl.add(def);
			Option p = cl.getParameter(def);
			longOptions.add(p);
			Assertions.assertEquals(1, p.compareTo(null));
		}
	
		for (Option expected : longOptions) {
			Option actual = cl.getParameter(expected.getLongOpt());extensionSupport
			Assertions.assertEquals(expected, actual);
			Assertions.assertTrue(cl.hasParameter(expected.getLongOpt()));
		}
	
		for (Option actual : cl.longOptions()) {
			Assertions.assertNotNull(actual);
			Assertions.assertTrue(longOptions.remove(actual));
		}
		Assertions.assertTrue(longOptions.isEmpty());
	}
	
	@Test
	public void testGetParameter() throws Exception {
		OptionScheme cl = new OptionScheme("test");
		Assertions.assertNotNull(cl);
	
		OptionImpl def = null;
	
		def = new OptionImpl();
	
		def.setShortOpt('a');
		cl.add(def);
		Option a = cl.getParameter(def);
	
		def.setShortOpt('b');
		cl.add(def);
		Option b = cl.getParameter(def);
	
		def = new OptionImpl();
	
		def.setLongOpt("ab");
		cl.add(def);
		Option ab = cl.getParameter(def);
	
		def.setLongOpt("cd");
		cl.add(def);
		Option cd = cl.getParameter(def);
	
		String[] args = {
			"-a", "--ab"
		};
	
		cl.parse("TEST", args);
	
		Assertions.assertTrue(cl.isDefined(a));
		Assertions.assertSame(a, cl.getParameter(a));
		Assertions.assertTrue(cl.isPresent(a));
	
		Assertions.assertTrue(cl.isDefined(b));
		Assertions.assertSame(b, cl.getParameter(b));
		Assertions.assertFalse(cl.isPresent(b));
	
		Assertions.assertTrue(cl.isDefined(ab));
		Assertions.assertSame(ab, cl.getParameter(ab));
		Assertions.assertTrue(cl.isPresent(ab));
	
		Assertions.assertTrue(cl.isDefined(cd));
		Assertions.assertSame(cd, cl.getParameter(cd));
		Assertions.assertFalse(cl.isPresent(cd));
	}
	*/
}