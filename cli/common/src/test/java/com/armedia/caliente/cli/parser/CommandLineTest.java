package com.armedia.caliente.cli.parser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
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
		} catch (NullPointerException e) {
			// All is well
		}

		def = new MutableParameterDefinition();
		try {
			cl.define(def);
			Assert.fail("Did not fail with no short or long options");
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
				} catch (IllegalArgumentException e) {
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
		} catch (IllegalArgumentException e) {
			// All is well
		}

		def.setLongOpt("a");
		try {
			cl.define(def);
			Assert.fail("Did not fail with a length-1 long option");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		def.setLongOpt("  ");
		try {
			cl.define(def);
			Assert.fail("Did not fail with a long option with spaces");
		} catch (IllegalArgumentException e) {
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
	}

	@Test
	public void testGetBoolean() {
	}

	@Test
	public void testGetInteger() {
	}

	@Test
	public void testGetLong() {
	}

	@Test
	public void testGetFloat() {
	}

	@Test
	public void testGetDouble() {
	}

	@Test
	public void testGetString() {
	}

	@Test
	public void testIsPresent() {
	}

	@Test
	public void testGetRemainingParameters() {
	}
}