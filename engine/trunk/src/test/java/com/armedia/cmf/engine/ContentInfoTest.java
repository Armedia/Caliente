package com.armedia.cmf.engine;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class ContentInfoTest {

	@Test
	public void testContentInfoString() {
		new ContentInfo(null);
		new ContentInfo(UUID.randomUUID().toString());
	}

	@Test
	public void testContentInfoStringString() {
		String q = UUID.randomUUID().toString();
		ContentInfo a = new ContentInfo(q);
		a.setProperty(",", ",");
		a.setProperty("=", ",");
		a.setProperty(",", "=");
		a.setProperty("=,", "=");
		for (int i = 0; i < 10; i++) {
			a.setProperty(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		}
		String p = a.encodeProperties();
		ContentInfo b = new ContentInfo(q, p);
		Assert.assertEquals(a.getPropertyCount(), b.getPropertyCount());
		for (String s : a.getPropertyNames()) {
			Assert.assertEquals(String.format("Property mismatch for [%s]", s), a.getProperty(s), b.getProperty(s));
		}

		String[] bad = {
			"a", "a,b", "a|b", "|||||",
		};

		for (String s : bad) {
			try {
				new ContentInfo(q, s);
				Assert.fail(String.format("Did not fail with illegal string [%s]", s));
			} catch (IllegalArgumentException e) {
				// All is well
			}
		}

		new ContentInfo(null, null);
	}

	@Test
	public void testGetQualifier() {
		ContentInfo a = null;
		String q = null;

		a = new ContentInfo(null);
		Assert.assertNull(a.getQualifier());

		q = UUID.randomUUID().toString();
		a = new ContentInfo(q);
		Assert.assertEquals(q, a.getQualifier());
	}

	@Test
	public void testSetProperty() {
		ContentInfo a = new ContentInfo(UUID.randomUUID().toString());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			Assert.assertEquals(i, a.getPropertyCount());
			a.setProperty(k, v);
			Assert.assertEquals(i + 1, a.getPropertyCount());
			Assert.assertTrue(a.hasProperty(k));
			Assert.assertEquals(v, a.getProperty(k));
		}
	}

	@Test
	public void testClearProperty() {
		ContentInfo a = new ContentInfo(UUID.randomUUID().toString());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			a.setProperty(k, v);
		}
		int i = a.getPropertyCount();
		for (String s : a.getPropertyNames()) {
			Assert.assertTrue(a.hasProperty(s));
			a.clearProperty(s);
			Assert.assertFalse(a.hasProperty(s));
			Assert.assertNull(a.getProperty(s));
			Assert.assertEquals(--i, a.getPropertyCount());
		}
	}

	@Test
	public void testClearAllProperties() {
		ContentInfo a = new ContentInfo(UUID.randomUUID().toString());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			a.setProperty(k, v);
		}
		Assert.assertEquals(100, a.getPropertyCount());
		a.clearAllProperties();
		Assert.assertEquals(0, a.getPropertyCount());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			Assert.assertFalse(a.hasProperty(k));
		}
	}

	@Test
	public void testGetPropertyNames() {
		ContentInfo a = new ContentInfo(UUID.randomUUID().toString());
		Set<String> names = new HashSet<String>();
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			a.setProperty(k, v);
			names.add(k);
		}
		Assert.assertEquals(names, a.getPropertyNames());
	}

	@Test
	public void testEncodeProperties() {
		ContentInfo a = new ContentInfo(UUID.randomUUID().toString());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key %03d", i);
			String v = String.format("value %03d", i);
			a.setProperty(k, v);
		}
		String encoded = a.encodeProperties();
		ContentInfo b = new ContentInfo(a.getQualifier(), encoded);
		Assert.assertEquals(a.getPropertyCount(), b.getPropertyCount());
		for (String n : a.getPropertyNames()) {
			Assert.assertEquals(a.getProperty(n), b.getProperty(n));
		}
	}
}