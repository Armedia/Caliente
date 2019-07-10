/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.store;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CmfContentStreamTest {

	@Test
	public void testContentStreamString() {
		new CmfContentStream(0, null);
		new CmfContentStream(0, UUID.randomUUID().toString());
	}

	@Test
	public void testGetIndex() {
		CmfContentStream a = null;

		for (int i = 0; i < 10000; i++) {
			a = new CmfContentStream(i);
			Assertions.assertEquals(i, a.getIndex());
		}
		for (int i = -100; i <= 0; i++) {
			a = new CmfContentStream(i);
			Assertions.assertEquals(0, a.getIndex());
		}

		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 1000; i++) {
			int index = r.nextInt(Integer.MAX_VALUE);
			a = new CmfContentStream(index);
			Assertions.assertEquals(index, a.getIndex());
		}
	}

	@Test
	public void testGetRenditionIdentifier() {
		CmfContentStream a = null;
		String q = null;

		a = new CmfContentStream(0, null);
		Assertions.assertNotNull(a.getRenditionIdentifier());
		Assertions.assertEquals(CmfContentStream.DEFAULT_RENDITION, a.getRenditionIdentifier());

		q = UUID.randomUUID().toString();
		a = new CmfContentStream(0, q);
		Assertions.assertEquals(q, a.getRenditionIdentifier());
	}

	@Test
	public void testGetRenditionPage() {
		CmfContentStream a = null;
		int v = -1;

		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 100; i++) {
			v = r.nextInt(100000);
			a = new CmfContentStream(0, v);
			Assertions.assertEquals(v, a.getRenditionPage());
		}
	}

	@Test
	public void testGetRenditionIdAndPage() {
		CmfContentStream a = null;
		String id = null;
		int page = -1;

		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 100; i++) {
			page = r.nextInt(100000);
			a = new CmfContentStream(0, null, page);
			Assertions.assertNotNull(a.getRenditionIdentifier());
			Assertions.assertEquals(CmfContentStream.DEFAULT_RENDITION, a.getRenditionIdentifier());
			Assertions.assertEquals(page, a.getRenditionPage());

			id = UUID.randomUUID().toString();
			a = new CmfContentStream(0, id, page);
			Assertions.assertEquals(id, a.getRenditionIdentifier());
			Assertions.assertEquals(page, a.getRenditionPage());
		}
	}

	@Test
	public void testGetRenditionIdAndPageAndModifier() {
		CmfContentStream a = null;
		String id = null;
		int page = -1;

		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 100; j++) {
				page = r.nextInt(100000);
				String modifier = String.format("modifier-%05X", j);
				a = new CmfContentStream(0, null, page);
				Assertions.assertNotNull(a.getRenditionIdentifier());
				Assertions.assertEquals(CmfContentStream.DEFAULT_RENDITION, a.getRenditionIdentifier());
				Assertions.assertEquals(page, a.getRenditionPage());
				Assertions.assertEquals("", a.getModifier());

				a = new CmfContentStream(0, null, page, null);
				Assertions.assertNotNull(a.getRenditionIdentifier());
				Assertions.assertEquals(CmfContentStream.DEFAULT_RENDITION, a.getRenditionIdentifier());
				Assertions.assertEquals(page, a.getRenditionPage());
				Assertions.assertEquals("", a.getModifier());

				a = new CmfContentStream(0, null, page, modifier);
				Assertions.assertNotNull(a.getRenditionIdentifier());
				Assertions.assertEquals(CmfContentStream.DEFAULT_RENDITION, a.getRenditionIdentifier());
				Assertions.assertEquals(page, a.getRenditionPage());
				Assertions.assertEquals(modifier, a.getModifier());

				id = UUID.randomUUID().toString();
				a = new CmfContentStream(0, id, page, modifier);
				Assertions.assertEquals(id, a.getRenditionIdentifier());
				Assertions.assertEquals(page, a.getRenditionPage());
				Assertions.assertEquals(modifier, a.getModifier());
			}
		}
	}

	@Test
	public void testSetProperty() {
		CmfContentStream a = new CmfContentStream(0, UUID.randomUUID().toString());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			Assertions.assertEquals(i, a.getPropertyCount());
			a.setProperty(k, v);
			Assertions.assertEquals(i + 1, a.getPropertyCount());
			Assertions.assertTrue(a.hasProperty(k));
			Assertions.assertEquals(v, a.getProperty(k));
		}
	}

	@Test
	public void testClearProperty() {
		CmfContentStream a = new CmfContentStream(0, UUID.randomUUID().toString());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			a.setProperty(k, v);
		}
		int i = a.getPropertyCount();
		for (String s : a.getPropertyNames()) {
			Assertions.assertTrue(a.hasProperty(s));
			a.clearProperty(s);
			Assertions.assertFalse(a.hasProperty(s));
			Assertions.assertNull(a.getProperty(s));
			Assertions.assertEquals(--i, a.getPropertyCount());
		}
	}

	@Test
	public void testClearAllProperties() {
		CmfContentStream a = new CmfContentStream(0, UUID.randomUUID().toString());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			a.setProperty(k, v);
		}
		Assertions.assertEquals(100, a.getPropertyCount());
		a.clearAllProperties();
		Assertions.assertEquals(0, a.getPropertyCount());
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			Assertions.assertFalse(a.hasProperty(k));
		}
	}

	@Test
	public void testGetPropertyNames() {
		CmfContentStream a = new CmfContentStream(0, UUID.randomUUID().toString());
		Set<String> names = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			String k = String.format("key-%03d", i);
			String v = String.format("value-%03d", i);
			a.setProperty(k, v);
			names.add(k);
		}
		Assertions.assertEquals(names, a.getPropertyNames());
	}
}