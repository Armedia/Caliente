package com.armedia.cmf.storage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class CmfContentInfoTest {

	@Test
	public void testContentInfoString() {
		new CmfContentInfo(null);
		new CmfContentInfo(UUID.randomUUID().toString());
	}

	@Test
	public void testGetQualifier() {
		CmfContentInfo a = null;
		String q = null;

		a = new CmfContentInfo(null);
		Assert.assertNull(a.getQualifier());

		q = UUID.randomUUID().toString();
		a = new CmfContentInfo(q);
		Assert.assertEquals(q, a.getQualifier());
	}

	@Test
	public void testSetProperty() {
		CmfContentInfo a = new CmfContentInfo(UUID.randomUUID().toString());
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
		CmfContentInfo a = new CmfContentInfo(UUID.randomUUID().toString());
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
		CmfContentInfo a = new CmfContentInfo(UUID.randomUUID().toString());
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
		CmfContentInfo a = new CmfContentInfo(UUID.randomUUID().toString());
		Set<String> names = new HashSet<String>();
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			a.setProperty(k, v);
			names.add(k);
		}
		Assert.assertEquals(names, a.getPropertyNames());
	}
}