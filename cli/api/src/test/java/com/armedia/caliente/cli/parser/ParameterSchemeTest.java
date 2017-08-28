package com.armedia.caliente.cli.parser;

public class ParameterSchemeTest {

	/*
	@Test
	public void testConstructor() {
		ParameterScheme cl = new ParameterScheme("test");
		Assert.assertNotNull(cl);
	}
	
	@Test
	public void testDefine() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
		Assert.assertNotNull(cl);
	
		ParameterImpl def = null;
	
		try {
			cl.addParameter(def);
			Assert.fail("Did not fail with a null parameter");
		} catch (InvalidParameterException e) {
			// All is well
		}
	
		def = new ParameterImpl();
		try {
			cl.addParameter(def);
			Assert.fail("Did not fail with no short or long options");
		} catch (InvalidParameterException e) {
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
				def = new ParameterImpl();
				def.setShortOpt(c);
				if (longOpt != null) {
					def.setLongOpt(longOpt);
				}
				try {
					cl.addParameter(def);
					Parameter p = cl.getParameter(def);
					if (!Character.isLetterOrDigit(c)) {
						Assert.fail(String.format("Did not fail with illegal character [%s]", c));
					}
					Assert.assertTrue(Parameter.isIdentical(def, p));
				} catch (InvalidParameterException e) {
					if (Character.isLetterOrDigit(c)) {
						Assert.fail(String.format("Failed with legal character [%s]", c));
					}
				} catch (DuplicateParameterException e) {
					if (c != '?') {
						if (longOpt == null) {
							Assert.fail(
								String.format("Duplicate exception caught when no duplicate was in play (%s)", c));
						}
					}
				}
			}
		}
	
		def = new ParameterImpl();
		def.setLongOpt("");
		try {
			cl.addParameter(def);
			Assert.fail("Did not fail with an empty long option");
		} catch (InvalidParameterException e) {
			// All is well
		}
	
		def.setLongOpt("a");
		try {
			cl.addParameter(def);
			Assert.fail("Did not fail with a length-1 long option");
		} catch (InvalidParameterException e) {
			// All is well
		}
	
		def.setLongOpt("  ");
		try {
			cl.addParameter(def);
			Assert.fail("Did not fail with a long option with spaces");
		} catch (InvalidParameterException e) {
			// All is well
		}
	
		def.setLongOpt("ab");
		cl.addParameter(def);
		Parameter p = cl.getParameter(def);
		Assert.assertEquals(p, p);
		Assert.assertNotEquals(p, null);
		Assert.assertNotEquals(p, "");
		// Assert.assertSame(cl, p.getParameterSchemeValues());
		Assert.assertTrue(Parameter.isIdentical(def, p));
	
		cl.addParameter(def);
		Parameter p2 = cl.getParameter(def);
		Assert.assertSame(p, p2);
	
		def.setLongOpt("cd");
		cl.addParameter(def);
		p2 = cl.getParameter(def);
		// Assert.assertSame(cl, p2.getParameterSchemeValues());
		Assert.assertNotSame(p, p2);
		Assert.assertTrue(Parameter.isIdentical(def, p2));
		Assert.assertNotEquals(p, p2);
	
		def.setShortOpt('x');
		try {
			cl.addParameter(def);
			p2 = cl.getParameter(def);
			Assert.fail(String.format("Did not fail with a duplicate option"));
		} catch (DuplicateParameterException e) {
			// All is well
		}
	}
	
	@Test
	public void testIterator() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
		Assert.assertNotNull(cl);
	
		ParameterImpl def = new ParameterImpl();
		Map<String, Parameter> m = new HashMap<>();
		for (int i = 0; i < 100; i++) {
			def.setLongOpt(String.format("long-%04x", i));
			cl.addParameter(def);
			Parameter p = cl.getParameter(def);
			m.put(def.getLongOpt(), p);
		}
	
		for (Parameter p : cl.getParameters()) {
			final String longOpt = p.getLongOpt();
			if ("help".equals(longOpt)) {
				// We're OK here...
				continue;
			}
			Assert.assertTrue(String.format("Unexpected long option [%s]", longOpt), m.containsKey(longOpt));
		}
	}
	
	@Test
	public void testHelp() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
		Assert.assertTrue(cl.hasHelpParameter());
	
		Parameter help = cl.getHelpParameter();
		Assert.assertNotNull(help);
		Assert.assertNotNull(help.getKey());
	
		String[] args = {
			"-a", "true", //
			"-b", "false", //
			"-c", //
		};
	
		ParameterImpl def = new ParameterImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.define(def);
	
		cl.parse("TEST", args);
	
		def.setMaxValueCount(1);
		def.setShortOpt('c');
		cl.define(def);
		cl.parse("TEST", args);
	
		args = new String[] {
			"-a", "true", //
			"-c", "false", //
			"-?"
		};
	
		cl.parse("TEST", args);
		Assert.assertTrue(cl.isHelpRequested());
		Assert.assertNotNull(cl.getHelpMessage());
	
		ParameterScheme cl2 = new ParameterScheme(false);
		Assert.assertFalse(cl2.hasHelpParameter());
		Assert.assertNull(cl2.getHelpParameter());
	
		Parameter p2prev = null;
		for (Parameter p : cl) {
			Parameter p2 = cl2.define(p);
			Assert.assertNotEquals(p, p2);
			Assert.assertNotEquals(p, p2prev);
			p2prev = p2;
		}
	
		try {
			cl2.parse("TEST", args);
		} catch (ParameterSchemeParseException e) {
			// we're good, this is expected
			Assert.assertNull(e.getHelp());
		}
	}
	
	@Test
	public void testGetBoolean() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
	
		String[] args = {
			"-a", "true", //
			"-b", "false", //
			"-c", "true,false", //
			"-d", "garbage", //
			"-f", //
		};
	
		ParameterImpl def = new ParameterImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.addParameter(def);
		Parameter a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.addParameter(def);
		Parameter b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.addParameter(def);
		Parameter d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.addParameter(def);
		Parameter e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.addParameter(def);
		Parameter c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.addParameter(def);
		Parameter f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assert.assertTrue("a", a.getBoolean());
		Assert.assertTrue("a-def", a.getBoolean(false));
		Assert.assertFalse("b", b.getBoolean());
		Assert.assertFalse("b-def", b.getBoolean(true));
		Assert.assertEquals("a-dual", b.getBoolean(), cl.getBoolean(b));
		Assert.assertEquals("a-dual-def", b.getBoolean(false), cl.getBoolean(b, false));
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
		Assert.assertNull("e-all", e.getAllBooleans());
		Assert.assertTrue("e-def-true", e.getBoolean(true));
		Assert.assertFalse("e-def-false", e.getBoolean(false));
		Assert.assertNull("f", f.getBoolean());
		Assert.assertTrue("f-all", f.getAllBooleans().isEmpty());
	}
	
	@Test
	public void testGetInteger() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
	
		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
			"-f", //
		};
	
		ParameterImpl def = new ParameterImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.addParameter(def);
		Parameter a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.addParameter(def);
		Parameter b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.addParameter(def);
		Parameter d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.addParameter(def);
		Parameter e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.addParameter(def);
		Parameter c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.addParameter(def);
		Parameter f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assert.assertEquals("a", Integer.valueOf(1), a.getInteger());
		Assert.assertEquals("a-def", 1, a.getInteger(2));
		Assert.assertEquals("a-dual", b.getInteger(), cl.getInteger(b));
		Assert.assertEquals("a-dual-def", b.getInteger(2), cl.getInteger(b, 2));
		Assert.assertEquals("b", Integer.valueOf(2), b.getInteger());
		Assert.assertEquals("b-def", 2, b.getInteger(3));
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
		Assert.assertNull("e-all", e.getAllIntegers());
		Assert.assertEquals("e-def-max", Integer.MAX_VALUE, e.getInteger(Integer.MAX_VALUE));
		Assert.assertEquals("e-def-min", Integer.MIN_VALUE, e.getInteger(Integer.MIN_VALUE));
		for (int i = 0; i < 100; i++) {
			int r = UUID.randomUUID().hashCode();
			Assert.assertEquals("e-def", r, e.getInteger(r));
		}
		Assert.assertNull("f", f.getInteger());
		Assert.assertTrue("f-all", f.getAllIntegers().isEmpty());
	}
	
	@Test
	public void testGetLong() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
	
		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
			"-f", //
		};
	
		ParameterImpl def = new ParameterImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.addParameter(def);
		Parameter a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.addParameter(def);
		Parameter b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.addParameter(def);
		Parameter d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.addParameter(def);
		Parameter e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.addParameter(def);
		Parameter c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.addParameter(def);
		Parameter f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assert.assertEquals("a", Long.valueOf(1), a.getLong());
		Assert.assertEquals("a-def", 1, a.getLong(2));
		Assert.assertEquals("b", Long.valueOf(2), b.getLong());
		Assert.assertEquals("b-def", 2, b.getLong(3));
		Assert.assertEquals("a-dual", b.getLong(), cl.getLong(b));
		Assert.assertEquals("a-dual-def", b.getLong(2), cl.getLong(b, 2));
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
		Assert.assertNull("e-all", e.getAllLongs());
		Assert.assertEquals("e-def-max", Long.MAX_VALUE, e.getLong(Long.MAX_VALUE));
		Assert.assertEquals("e-def-min", Long.MIN_VALUE, e.getLong(Long.MIN_VALUE));
		for (int i = 0; i < 100; i++) {
			long r = UUID.randomUUID().hashCode() * UUID.randomUUID().hashCode();
			Assert.assertEquals("e-def", r, e.getLong(r));
		}
		Assert.assertNull("f", f.getLong());
		Assert.assertTrue("f-all", f.getAllLongs().isEmpty());
	}
	
	@Test
	public void testGetFloat() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
	
		String[] args = {
			"-a", "1.1", //
			"-b", "2.2", //
			"-c", "3.3,4.4", //
			"-d", "5.5", //
			"-f", //
		};
	
		ParameterImpl def = new ParameterImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.addParameter(def);
		Parameter a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.addParameter(def);
		Parameter b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.addParameter(def);
		Parameter d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.addParameter(def);
		Parameter e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.addParameter(def);
		Parameter c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.addParameter(def);
		Parameter f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assert.assertEquals("a", Float.valueOf(1.1f), a.getFloat());
		Assert.assertEquals("a-def", Float.valueOf(1.1f), a.getFloat(2.2f), 0.0f);
		Assert.assertEquals("b", Float.valueOf(2.2f), b.getFloat());
		Assert.assertEquals("b-def", Float.valueOf(2.2f), b.getFloat(1.1f), 0.0f);
		Assert.assertEquals("a-dual", b.getFloat(), cl.getFloat(b));
		Assert.assertEquals("a-dual-def", b.getFloat(2), cl.getFloat(b, 2), 0.0f);
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
		Assert.assertNull("e-all", e.getAllFloats());
		Assert.assertEquals("e-def-max", Float.MAX_VALUE, e.getFloat(Float.MAX_VALUE), 0.0f);
		Assert.assertEquals("e-def-min", Float.MIN_VALUE, e.getFloat(Float.MIN_VALUE), 0.0f);
		Assert.assertEquals("e-def-nan", Float.NaN, e.getFloat(Float.NaN), 0.0f);
		Assert.assertEquals("e-def-+inf", Float.POSITIVE_INFINITY, e.getFloat(Float.POSITIVE_INFINITY), 0.0f);
		Assert.assertEquals("e-def--inf", Float.NEGATIVE_INFINITY, e.getFloat(Float.NEGATIVE_INFINITY), 0.0f);
		Assert.assertNull("f", f.getFloat());
		Assert.assertTrue("f-all", f.getAllFloats().isEmpty());
	}
	
	@Test
	public void testGetDouble() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
	
		String[] args = {
			"-a", "1.1", //
			"-b", "2.2", //
			"-c", "3.3,4.4", //
			"-d", "5.5", //
			"-f", //
		};
	
		ParameterImpl def = new ParameterImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.addParameter(def);
		Parameter a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.addParameter(def);
		Parameter b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.addParameter(def);
		Parameter d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.addParameter(def);
		Parameter e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.addParameter(def);
		Parameter c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.addParameter(def);
		Parameter f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assert.assertEquals("a", Double.valueOf(1.1), a.getDouble());
		Assert.assertEquals("a-def", Double.valueOf(1.1), a.getDouble(2.2), 0.0);
		Assert.assertEquals("b", Double.valueOf(2.2), b.getDouble());
		Assert.assertEquals("b-def", Double.valueOf(2.2), b.getDouble(3.3), 0.0);
		Assert.assertEquals("a-dual", b.getDouble(), cl.getDouble(b));
		Assert.assertEquals("a-dual-def", b.getDouble(2), cl.getDouble(b, 2), 0.0);
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
		Assert.assertNull("e-all", e.getAllDoubles());
		Assert.assertEquals("e-def-max", Double.MAX_VALUE, e.getDouble(Double.MAX_VALUE), 0.0);
		Assert.assertEquals("e-def-min", Double.MIN_VALUE, e.getDouble(Double.MIN_VALUE), 0.0);
		Assert.assertEquals("e-def-nan", Double.NaN, e.getDouble(Double.NaN), 0.0);
		Assert.assertEquals("e-def-+inf", Double.POSITIVE_INFINITY, e.getDouble(Double.POSITIVE_INFINITY), 0.0);
		Assert.assertEquals("e-def--inf", Double.NEGATIVE_INFINITY, e.getDouble(Double.NEGATIVE_INFINITY), 0.0);
		Assert.assertNull("f", f.getDouble());
		Assert.assertTrue("f-all", f.getAllDoubles().isEmpty());
	}
	
	@Test
	public void testGetString() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
	
		String[] args = {
			"-a", "1", //
			"-b", "2", //
			"-c", "3,4", //
			"-d", "5", //
			"-f",
		};
	
		ParameterImpl def = new ParameterImpl();
		def.setMaxValueCount(1);
		def.setShortOpt('a');
		cl.addParameter(def);
		Parameter a = cl.getParameter(def);
		def.setShortOpt('b');
		cl.addParameter(def);
		Parameter b = cl.getParameter(def);
		def.setShortOpt('d');
		cl.addParameter(def);
		Parameter d = cl.getParameter(def);
		def.setShortOpt('e');
		cl.addParameter(def);
		Parameter e = cl.getParameter(def);
		def.setShortOpt('c');
		def.setMaxValueCount(-1);
		cl.addParameter(def);
		Parameter c = cl.getParameter(def);
		def.setShortOpt('f');
		def.setMaxValueCount(0);
		cl.addParameter(def);
		Parameter f = cl.getParameter(def);
	
		cl.parse("TEST", args);
	
		Assert.assertEquals("a", "1", a.getString());
		Assert.assertEquals("a-def", "1", a.getString("x"));
		Assert.assertEquals("b", "2", b.getString());
		Assert.assertEquals("b-def", "2", b.getString("x"));
		Assert.assertEquals("a-dual", b.getString(), cl.getString(b));
		Assert.assertEquals("a-dual-def", b.getString("x"), cl.getString(b, "x"));
		List<String> C = c.getAllStrings();
		Assert.assertNotNull(C);
		List<String> CS = c.getAllStrings();
		Assert.assertEquals(CS.size(), C.size());
		for (int i = 0; i < CS.size(); i++) {
			Assert.assertEquals(CS.get(i), C.get(i));
		}
		Assert.assertEquals("d", "5", d.getString());
		Assert.assertEquals("d", "5", d.getString("x"));
		Assert.assertNull("e", e.getString());
		Assert.assertNull("e-all", e.getAllStrings());
		for (int i = 0; i < 100; i++) {
			String s = UUID.randomUUID().toString();
			Assert.assertEquals("e-def", s, e.getString(s));
		}
		Assert.assertNull("f", f.getString());
		Assert.assertTrue("f-all", f.getAllStrings().isEmpty());
	}
	
	@Test
	public void testIsPresent() throws Exception {
		// Short options
		{
			ParameterScheme cl = new ParameterScheme("test");
	
			String[] args = {
				"-a", //
				"-b", "2", //
				"-c", "3,4", //
			};
	
			ParameterImpl def = new ParameterImpl();
			def.setShortOpt('a');
			cl.addParameter(def);
			Parameter a = cl.getParameter(def);
	
			def.setMaxValueCount(1);
			def.setShortOpt('b');
			cl.addParameter(def);
			Parameter b = cl.getParameter(def);
	
			def.setMaxValueCount(-1);
			def.setShortOpt('c');
			cl.addParameter(def);
			Parameter c = cl.getParameter(def);
	
			def.setShortOpt('d');
			cl.addParameter(def);
			Parameter d = cl.getParameter(def);
	
			cl.parse("TEST", args);
	
			Assert.assertTrue(a.isPresent());
			Assert.assertEquals(a.isPresent(), cl.isPresent(a));
			Assert.assertTrue(b.isPresent());
			Assert.assertTrue(c.isPresent());
			Assert.assertFalse(d.isPresent());
			Assert.assertEquals(d.isPresent(), cl.isPresent(d));
		}
		// Long options
		{
			ParameterScheme cl = new ParameterScheme("test");
	
			String[] args = {
				"--long-a", //
				"--long-b", "2", //
				"--long-c", "3,4", //
			};
	
			ParameterImpl def = new ParameterImpl();
			def.setLongOpt("long-a");
			cl.addParameter(def);
			Parameter a = cl.getParameter(def);
	
			def.setMaxValueCount(1);
			def.setLongOpt("long-b");
			cl.addParameter(def);
			Parameter b = cl.getParameter(def);
	
			def.setMaxValueCount(-1);
			def.setLongOpt("long-c");
			cl.addParameter(def);
			Parameter c = cl.getParameter(def);
	
			def.setLongOpt("long-d");
			cl.addParameter(def);
			Parameter d = cl.getParameter(def);
	
			cl.parse("TEST", args);
	
			Assert.assertTrue(a.isPresent());
			Assert.assertEquals(a.isPresent(), cl.isPresent(a));
			Assert.assertTrue(b.isPresent());
			Assert.assertTrue(c.isPresent());
			Assert.assertFalse(d.isPresent());
			Assert.assertEquals(d.isPresent(), cl.isPresent(d));
		}
	}
	
	@Test
	public void testGetRemainingParameters() throws Exception {
		// With remaining
		{
			ParameterScheme cl = new ParameterScheme("test");
	
			String[] args = {
				"-a", "1", //
				"b", "2", //
				"c", "3,4", //
				"d", "5", //
			};
	
			ParameterImpl def = new ParameterImpl();
			def.setMaxValueCount(1);
			def.setShortOpt('a');
			cl.addParameter(def);
			Parameter a = cl.getParameter(def);
	
			cl.parse("TEST", args);
	
			Assert.assertEquals("a", Integer.valueOf(1), a.getInteger());
			List<String> remaining = cl.getPositionalValues();
			Assert.assertEquals(6, remaining.size());
			for (int i = 2; i < args.length; i++) {
				Assert.assertEquals(args[i], remaining.get(i - 2));
			}
		}
		// Without remaining
		{
			ParameterScheme cl = new ParameterScheme("test");
	
			String[] args = {
				"-a", "1", //
			};
	
			ParameterImpl def = new ParameterImpl();
			def.setMaxValueCount(1);
			def.setShortOpt('a');
			cl.addParameter(def);
			Parameter a = cl.getParameter(def);
	
			cl.parse("TEST", args);
	
			Assert.assertEquals("a", Integer.valueOf(1), a.getInteger());
			List<String> remaining = cl.getPositionalValues();
			Assert.assertTrue(remaining.isEmpty());
		}
	}
	
	@Test
	public void testShortOptions() {
		ParameterScheme cl = new ParameterScheme(false);
		final Charset charset = Charset.forName("US-ASCII");
		ParameterImpl def = null;
		Set<Parameter> shortOptions = new HashSet<>();
		for (int i = 0; i < 255; i++) {
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(i);
			String s = new String(bb.array(), charset).trim();
			if (s.length() != 1) {
				continue;
			}
			char c = s.charAt(0);
			def = new ParameterImpl();
			def.setShortOpt(c);
			try {
				cl.addParameter(def);
				Parameter p = cl.getParameter(def);
				shortOptions.add(p);
				Assert.assertEquals(1, p.compareTo(null));
			} catch (InvalidParameterException e) {
				if (Character.isLetterOrDigit(c)) {
					Assert.fail(String.format("Failed with legal character [%s]", c));
				}
			} catch (DuplicateParameterException e) {
				Assert.fail(String.format("Duplicate exception caught when no duplicate was in play (%s)", c));
			}
		}
	
		for (Parameter expected : shortOptions) {
			Parameter actual = cl.getParameter(expected.getShortOpt());
			Assert.assertEquals(expected, actual);
			Assert.assertTrue(cl.hasParameter(expected.getShortOpt()));
		}
	
		for (Parameter actual : cl.shortOptions()) {
			Assert.assertNotNull(actual);
			Assert.assertTrue(shortOptions.remove(actual));
		}
		Assert.assertTrue(shortOptions.isEmpty());
	}
	
	@Test
	public void testLongOptions() throws Exception {
		ParameterScheme cl = new ParameterScheme(false);
		ParameterImpl def = null;
		Set<Parameter> longOptions = new HashSet<>();
		for (int i = 0; i < 255; i++) {
			def = new ParameterImpl();
			def.setLongOpt(String.format("long-%04x", i));
			cl.addParameter(def);
			Parameter p = cl.getParameter(def);
			longOptions.add(p);
			Assert.assertEquals(1, p.compareTo(null));
		}
	
		for (Parameter expected : longOptions) {
			Parameter actual = cl.getParameter(expected.getLongOpt());
			Assert.assertEquals(expected, actual);
			Assert.assertTrue(cl.hasParameter(expected.getLongOpt()));
		}
	
		for (Parameter actual : cl.longOptions()) {
			Assert.assertNotNull(actual);
			Assert.assertTrue(longOptions.remove(actual));
		}
		Assert.assertTrue(longOptions.isEmpty());
	}
	
	@Test
	public void testGetParameter() throws Exception {
		ParameterScheme cl = new ParameterScheme("test");
		Assert.assertNotNull(cl);
	
		ParameterImpl def = null;
	
		def = new ParameterImpl();
	
		def.setShortOpt('a');
		cl.addParameter(def);
		Parameter a = cl.getParameter(def);
	
		def.setShortOpt('b');
		cl.addParameter(def);
		Parameter b = cl.getParameter(def);
	
		def = new ParameterImpl();
	
		def.setLongOpt("ab");
		cl.addParameter(def);
		Parameter ab = cl.getParameter(def);
	
		def.setLongOpt("cd");
		cl.addParameter(def);
		Parameter cd = cl.getParameter(def);
	
		String[] args = {
			"-a", "--ab"
		};
	
		cl.parse("TEST", args);
	
		Assert.assertTrue(cl.isDefined(a));
		Assert.assertSame(a, cl.getParameter(a));
		Assert.assertTrue(cl.isPresent(a));
	
		Assert.assertTrue(cl.isDefined(b));
		Assert.assertSame(b, cl.getParameter(b));
		Assert.assertFalse(cl.isPresent(b));
	
		Assert.assertTrue(cl.isDefined(ab));
		Assert.assertSame(ab, cl.getParameter(ab));
		Assert.assertTrue(cl.isPresent(ab));
	
		Assert.assertTrue(cl.isDefined(cd));
		Assert.assertSame(cd, cl.getParameter(cd));
		Assert.assertFalse(cl.isPresent(cd));
	}
	*/
}