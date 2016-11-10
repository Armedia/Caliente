package com.armedia.caliente.cli.parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class CommandLineTest {

	@Test
	public void testConstructor() {
		CommandLine cl = new CommandLine();
		Assert.assertNotNull(cl);
	}

	@Test
	public void testDefine() throws Exception {
		CommandLine cl = new CommandLine();
		Assert.assertNotNull(cl);

		MutableParameterDefinition def = null;

		try {
			cl.define(def);
			Assert.fail("Did not fail with a null parameter");
		} catch (InvalidParameterDefinitionException e) {
			// All is well
		}

		def = new MutableParameterDefinition();
		try {
			cl.define(def);
			Assert.fail("Did not fail with no short or long options");
		} catch (InvalidParameterDefinitionException e) {
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
				def = new MutableParameterDefinition();
				def.setShortOpt(c);
				if (longOpt != null) {
					def.setLongOpt(longOpt);
				}
				try {
					Parameter p = cl.define(def);
					if (!Character.isLetterOrDigit(c)) {
						Assert.fail(String.format("Did not fail with illegal character [%s]", c));
					}
					Assert.assertSame(cl, p.getCLI());
					Assert.assertEquals(def, p.getDefinition());
				} catch (InvalidParameterDefinitionException e) {
					if (Character.isLetterOrDigit(c)) {
						Assert.fail(String.format("Failed with legal character [%s]", c));
					}
				} catch (DuplicateParameterDefinitionException e) {
					if (c != '?') {
						if (longOpt == null) {
							Assert.fail(
								String.format("Duplicate exception caught when no duplicate was in play (%s)", c));
						}
					}
				}

			}
		}

		def = new MutableParameterDefinition();
		def.setLongOpt("");
		try {
			cl.define(def);
			Assert.fail("Did not fail with an empty long option");
		} catch (InvalidParameterDefinitionException e) {
			// All is well
		}

		def.setLongOpt("a");
		try {
			cl.define(def);
			Assert.fail("Did not fail with a length-1 long option");
		} catch (InvalidParameterDefinitionException e) {
			// All is well
		}

		def.setLongOpt("  ");
		try {
			cl.define(def);
			Assert.fail("Did not fail with a long option with spaces");
		} catch (InvalidParameterDefinitionException e) {
			// All is well
		}

		def.setLongOpt("ab");
		Parameter p = cl.define(def);
		Assert.assertSame(cl, p.getCLI());
		Assert.assertEquals(def, p.getDefinition());

		Parameter p2 = cl.define(def);
		Assert.assertSame(p, p2);

		def.setLongOpt("cd");
		p2 = cl.define(def);
		Assert.assertSame(cl, p2.getCLI());
		Assert.assertNotSame(p, p2);
		Assert.assertEquals(def, p2.getDefinition());
		Assert.assertNotEquals(p.getDefinition(), p2.getDefinition());

		def.setShortOpt('x');
		try {
			p2 = cl.define(def);
			Assert.fail(String.format("Did not fail with a duplicate option"));
		} catch (DuplicateParameterDefinitionException e) {
			// All is well
		}
	}

	@Test
	public void testIterator() throws Exception {
		CommandLine cl = new CommandLine();
		Assert.assertNotNull(cl);

		MutableParameterDefinition def = new MutableParameterDefinition();
		Map<String, Parameter> m = new HashMap<>();
		for (int i = 0; i < 100; i++) {
			def.setLongOpt(String.format("long-%04x", i));
			Parameter p = cl.define(def);
			m.put(def.getLongOpt(), p);
		}

		for (Parameter p : cl) {
			final String longOpt = p.getDefinition().getLongOpt();
			if ("help".equals(longOpt)) {
				// We're OK here...
				continue;
			}
			Assert.assertTrue(String.format("Unexpected long option [%s]", longOpt), m.containsKey(longOpt));
		}
	}

	@Test
	public void testParse() {
		CommandLine cl = null;
		String[] args = null;
	}

	@Test
	public void testGetBoolean() throws Exception {
		CommandLine cl = new CommandLine();

		String[] args = {
			"-a", "true", //
			"-b", "false", //
			"-c", "true,false", //
			"-d", "garbage", //
		};

		MutableParameterDefinition def = new MutableParameterDefinition();
		def.setValueCount(1);
		def.setShortOpt('a');
		Parameter a = cl.define(def);
		def.setShortOpt('b');
		Parameter b = cl.define(def);
		def.setShortOpt('d');
		Parameter d = cl.define(def);
		def.setShortOpt('e');
		Parameter e = cl.define(def);
		def.setShortOpt('c');
		def.setValueCount(-1);
		Parameter c = cl.define(def);

		cl.parse(new CommonsCliParser(), "TEST", args);

		Assert.assertTrue("a", a.getBoolean());
		Assert.assertFalse("b", b.getBoolean());
		List<Boolean> C = c.getAllBooleans();
		Assert.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assert.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Boolean v = Boolean.valueOf(CS.get(i));
			Assert.assertEquals(v, C.get(i));
		}
		Assert.assertFalse("d", d.getBoolean());
		Assert.assertNull("e", e.getBoolean());
	}

	@Test
	public void testGetInteger() throws Exception {
		CommandLine cl = new CommandLine();

		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
		};

		MutableParameterDefinition def = new MutableParameterDefinition();
		def.setValueCount(1);
		def.setShortOpt('a');
		Parameter a = cl.define(def);
		def.setShortOpt('b');
		Parameter b = cl.define(def);
		def.setShortOpt('d');
		Parameter d = cl.define(def);
		def.setShortOpt('e');
		Parameter e = cl.define(def);
		def.setShortOpt('c');
		def.setValueCount(-1);
		Parameter c = cl.define(def);

		cl.parse(new CommonsCliParser(), "TEST", args);

		Assert.assertEquals("a", Integer.valueOf(1), a.getInteger());
		Assert.assertEquals("b", Integer.valueOf(2), b.getInteger());
		List<Integer> C = c.getAllIntegers();
		Assert.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assert.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Integer v = Integer.valueOf(CS.get(i));
			Assert.assertEquals(v, C.get(i));
		}
		Assert.assertEquals("d", Integer.valueOf(5), d.getInteger());
		Assert.assertNull("e", e.getInteger());
	}

	@Test
	public void testGetLong() throws Exception {
		CommandLine cl = new CommandLine();

		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
		};

		MutableParameterDefinition def = new MutableParameterDefinition();
		def.setValueCount(1);
		def.setShortOpt('a');
		Parameter a = cl.define(def);
		def.setShortOpt('b');
		Parameter b = cl.define(def);
		def.setShortOpt('d');
		Parameter d = cl.define(def);
		def.setShortOpt('e');
		Parameter e = cl.define(def);
		def.setShortOpt('c');
		def.setValueCount(-1);
		Parameter c = cl.define(def);

		cl.parse(new CommonsCliParser(), "TEST", args);

		Assert.assertEquals("a", Long.valueOf(1), a.getLong());
		Assert.assertEquals("b", Long.valueOf(2), b.getLong());
		List<Long> C = c.getAllLongs();
		Assert.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assert.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Long v = Long.valueOf(CS.get(i));
			Assert.assertEquals(v, C.get(i));
		}
		Assert.assertEquals("d", Long.valueOf(5), d.getLong());
		Assert.assertNull("e", e.getLong());
	}

	@Test
	public void testGetFloat() throws Exception {
		CommandLine cl = new CommandLine();

		String[] args = {
			"-a", "1.1", //
			"-b", "2.2", //
			"-c", "3.3,4.4", //
			"-d", "5.5", //
		};

		MutableParameterDefinition def = new MutableParameterDefinition();
		def.setValueCount(1);
		def.setShortOpt('a');
		Parameter a = cl.define(def);
		def.setShortOpt('b');
		Parameter b = cl.define(def);
		def.setShortOpt('d');
		Parameter d = cl.define(def);
		def.setShortOpt('e');
		Parameter e = cl.define(def);
		def.setShortOpt('c');
		def.setValueCount(-1);
		Parameter c = cl.define(def);

		cl.parse(new CommonsCliParser(), "TEST", args);

		Assert.assertEquals("a", Float.valueOf(1.1f), a.getFloat());
		Assert.assertEquals("b", Float.valueOf(2.2f), b.getFloat());
		List<Float> C = c.getAllFloats();
		Assert.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assert.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Float v = Float.valueOf(CS.get(i));
			Assert.assertEquals(v, C.get(i));
		}
		Assert.assertEquals("d", Float.valueOf(5.5f), d.getFloat());
		Assert.assertNull("e", e.getFloat());
	}

	@Test
	public void testGetDouble() throws Exception {
		CommandLine cl = new CommandLine();

		String[] args = {
			"-a", "1.1", //
			"-b", "2.2", //
			"-c", "3.3,4.4", //
			"-d", "5.5", //
		};

		MutableParameterDefinition def = new MutableParameterDefinition();
		def.setValueCount(1);
		def.setShortOpt('a');
		Parameter a = cl.define(def);
		def.setShortOpt('b');
		Parameter b = cl.define(def);
		def.setShortOpt('d');
		Parameter d = cl.define(def);
		def.setShortOpt('e');
		Parameter e = cl.define(def);
		def.setShortOpt('c');
		def.setValueCount(-1);
		Parameter c = cl.define(def);

		cl.parse(new CommonsCliParser(), "TEST", args);

		Assert.assertEquals("a", Double.valueOf(1.1), a.getDouble());
		Assert.assertEquals("b", Double.valueOf(2.2), b.getDouble());
		List<Double> C = c.getAllDoubles();
		Assert.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assert.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Double v = Double.valueOf(CS.get(i));
			Assert.assertEquals(v, C.get(i));
		}
		Assert.assertEquals("d", Double.valueOf(5.5), d.getDouble());
		Assert.assertNull("e", e.getDouble());
	}

	@Test
	public void testGetString() throws Exception {
		CommandLine cl = new CommandLine();

		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
		};

		MutableParameterDefinition def = new MutableParameterDefinition();
		def.setValueCount(1);
		def.setShortOpt('a');
		Parameter a = cl.define(def);
		def.setShortOpt('b');
		Parameter b = cl.define(def);
		def.setShortOpt('d');
		Parameter d = cl.define(def);
		def.setShortOpt('e');
		Parameter e = cl.define(def);
		def.setShortOpt('c');
		def.setValueCount(-1);
		Parameter c = cl.define(def);

		cl.parse(new CommonsCliParser(), "TEST", args);

		Assert.assertEquals("a", "1", a.getString());
		Assert.assertEquals("b", "2", b.getString());
		List<String> C = c.getAllStrings();
		Assert.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assert.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Assert.assertEquals(CS.get(i), C.get(i));
		}
		Assert.assertEquals("d", "5", d.getString());
		Assert.assertNull("e", e.getString());
	}

	@Test
	public void testIsPresent() throws Exception {
		// Short options
		{
			CommandLine cl = new CommandLine();

			String[] args = {
				"-a", //
				"-b", "2", //
				"-c", "3,4", //
			};

			MutableParameterDefinition def = new MutableParameterDefinition();
			def.setShortOpt('a');
			Parameter a = cl.define(def);

			def.setValueCount(1);
			def.setShortOpt('b');
			Parameter b = cl.define(def);

			def.setValueCount(-1);
			def.setShortOpt('c');
			Parameter c = cl.define(def);

			def.setShortOpt('d');
			Parameter d = cl.define(def);

			cl.parse(new CommonsCliParser(), "TEST", args);

			Assert.assertTrue(a.isPresent());
			Assert.assertTrue(b.isPresent());
			Assert.assertTrue(c.isPresent());
			Assert.assertFalse(d.isPresent());
		}
		// Long options
		{
			CommandLine cl = new CommandLine();

			String[] args = {
				"--long-a", //
				"--long-b", "2", //
				"--long-c", "3,4", //
			};

			MutableParameterDefinition def = new MutableParameterDefinition();
			def.setLongOpt("long-a");
			Parameter a = cl.define(def);

			def.setValueCount(1);
			def.setLongOpt("long-b");
			Parameter b = cl.define(def);

			def.setValueCount(-1);
			def.setLongOpt("long-c");
			Parameter c = cl.define(def);

			def.setLongOpt("long-d");
			Parameter d = cl.define(def);

			cl.parse(new CommonsCliParser(), "TEST", args);

			Assert.assertTrue(a.isPresent());
			Assert.assertTrue(b.isPresent());
			Assert.assertTrue(c.isPresent());
			Assert.assertFalse(d.isPresent());
		}
	}

	@Test
	public void testGetRemainingParameters() throws Exception {
		// With remaining
		{
			CommandLine cl = new CommandLine();

			String[] args = {
				"-a", "1", //
				"b", "2", //
				"c", "3,4", //
				"d", "5", //
			};

			MutableParameterDefinition def = new MutableParameterDefinition();
			def.setValueCount(1);
			def.setShortOpt('a');
			Parameter a = cl.define(def);

			cl.parse(new CommonsCliParser(), "TEST", args);

			Assert.assertEquals("a", Integer.valueOf(1), a.getInteger());
			List<String> remaining = cl.getRemainingParameters();
			Assert.assertEquals(6, remaining.size());
			for (int i = 2; i < args.length; i++) {
				Assert.assertEquals(args[i], remaining.get(i - 2));
			}
		}
		// Without remaining
		{
			CommandLine cl = new CommandLine();

			String[] args = {
				"-a", "1", //
			};

			MutableParameterDefinition def = new MutableParameterDefinition();
			def.setValueCount(1);
			def.setShortOpt('a');
			Parameter a = cl.define(def);

			cl.parse(new CommonsCliParser(), "TEST", args);

			Assert.assertEquals("a", Integer.valueOf(1), a.getInteger());
			List<String> remaining = cl.getRemainingParameters();
			Assert.assertTrue(remaining.isEmpty());
		}
	}
}