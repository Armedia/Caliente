package com.armedia.caliente.store;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.store.CmfContentInfo;

public class CmfContentInfoTest {

	@Test
	public void testContentInfoString() {
		new CmfContentInfo(null);
		new CmfContentInfo(UUID.randomUUID().toString());
	}

	@Test
	public void testGetRenditionIdentifier() {
		CmfContentInfo a = null;
		String q = null;

		a = new CmfContentInfo(null);
		Assert.assertNotNull(a.getRenditionIdentifier());
		Assert.assertEquals(CmfContentInfo.DEFAULT_RENDITION, a.getRenditionIdentifier());

		q = UUID.randomUUID().toString();
		a = new CmfContentInfo(q);
		Assert.assertEquals(q, a.getRenditionIdentifier());
	}

	@Test
	public void testGetRenditionPage() {
		CmfContentInfo a = null;
		int v = -1;

		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 100; i++) {
			v = r.nextInt(100000);
			a = new CmfContentInfo(v);
			Assert.assertEquals(v, a.getRenditionPage());
		}
	}

	@Test
	public void testGetRenditionIdAndPage() {
		CmfContentInfo a = null;
		String id = null;
		int page = -1;

		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 100; i++) {
			page = r.nextInt(100000);
			a = new CmfContentInfo(null, page);
			Assert.assertNotNull(a.getRenditionIdentifier());
			Assert.assertEquals(CmfContentInfo.DEFAULT_RENDITION, a.getRenditionIdentifier());
			Assert.assertEquals(page, a.getRenditionPage());

			id = UUID.randomUUID().toString();
			a = new CmfContentInfo(id, page);
			Assert.assertEquals(id, a.getRenditionIdentifier());
			Assert.assertEquals(page, a.getRenditionPage());
		}
	}

	@Test
	public void testGetRenditionIdAndPageAndModifier() {
		CmfContentInfo a = null;
		String id = null;
		int page = -1;

		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 100; j++) {
				page = r.nextInt(100000);
				String modifier = String.format("modifier-%05X", j);
				a = new CmfContentInfo(null, page);
				Assert.assertNotNull(a.getRenditionIdentifier());
				Assert.assertEquals(CmfContentInfo.DEFAULT_RENDITION, a.getRenditionIdentifier());
				Assert.assertEquals(page, a.getRenditionPage());
				Assert.assertEquals("", a.getModifier());

				a = new CmfContentInfo(null, page, null);
				Assert.assertNotNull(a.getRenditionIdentifier());
				Assert.assertEquals(CmfContentInfo.DEFAULT_RENDITION, a.getRenditionIdentifier());
				Assert.assertEquals(page, a.getRenditionPage());
				Assert.assertEquals("", a.getModifier());

				a = new CmfContentInfo(null, page, modifier);
				Assert.assertNotNull(a.getRenditionIdentifier());
				Assert.assertEquals(CmfContentInfo.DEFAULT_RENDITION, a.getRenditionIdentifier());
				Assert.assertEquals(page, a.getRenditionPage());
				Assert.assertEquals(modifier, a.getModifier());

				id = UUID.randomUUID().toString();
				a = new CmfContentInfo(id, page, modifier);
				Assert.assertEquals(id, a.getRenditionIdentifier());
				Assert.assertEquals(page, a.getRenditionPage());
				Assert.assertEquals(modifier, a.getModifier());
			}
		}
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