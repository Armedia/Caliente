/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.tools.dfc;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;

public class DctmCollectionIteratorTest {

	@Test
	public void testDctmCollectionIterator() throws Exception {
		IDfCollection c = null;
		List<IDfTypedObject> l = new ArrayList<>();
		DfException thrown = null;

		// Empty collection
		c = EasyMock.createMock(IDfCollection.class);
		EasyMock.expect(c.next()).andReturn(false).anyTimes();
		c.close();
		EasyMock.expectLastCall().once();
		EasyMock.replay(c);

		try (DfcCollectionIterator it = new DfcCollectionIterator(c)) {
			Assertions.assertFalse(it.hasNext());
			Assertions.assertThrows(NoSuchElementException.class, () -> it.next());
		}
		EasyMock.verify(c);

		// 10 objects
		c = EasyMock.createMock(IDfCollection.class);
		for (int i = 0; i < 10; i++) {
			IDfTypedObject o = EasyMock.createMock(IDfTypedObject.class);
			EasyMock.replay(o);
			l.add(o);

			EasyMock.expect(c.next()).andReturn(true).once();
			EasyMock.expect(c.getTypedObject()).andReturn(o).once();
		}
		EasyMock.expect(c.next()).andReturn(false).anyTimes();
		c.close();
		EasyMock.expectLastCall().once();
		EasyMock.replay(c);

		try (DfcCollectionIterator it = new DfcCollectionIterator(c)) {
			int p = 0;
			while (it.hasNext()) {
				IDfTypedObject a = l.get(p++);
				IDfTypedObject b = it.next();
				Assertions.assertSame(a, b);
			}
			Assertions.assertFalse(it.hasNext());
			Assertions.assertThrows(NoSuchElementException.class, () -> it.next());
		}
		EasyMock.verify(c);

		// Exception on next()
		c = EasyMock.createMock(IDfCollection.class);
		thrown = new DfException(UUID.randomUUID().toString());
		EasyMock.expect(c.next()).andThrow(thrown).once();
		c.close();
		EasyMock.expectLastCall().once();
		EasyMock.replay(c);

		try (DfcCollectionIterator it = new DfcCollectionIterator(c)) {
			try {
				it.hasNext();
				Assertions.fail("Did not fail with a cascaded exception");
			} catch (RuntimeException e) {
				Assertions.assertSame(thrown, e.getCause());
			}
		}
		EasyMock.verify(c);

		// Exception on next()
		c = EasyMock.createMock(IDfCollection.class);
		thrown = new DfException(UUID.randomUUID().toString());
		EasyMock.expect(c.next()).andReturn(true).once();
		EasyMock.expect(c.getTypedObject()).andThrow(thrown).once();
		c.close();
		EasyMock.expectLastCall().once();
		EasyMock.replay(c);

		try (DfcCollectionIterator it = new DfcCollectionIterator(c)) {
			try {
				it.hasNext();
				Assertions.fail("Did not fail with a cascaded exception");
			} catch (RuntimeException e) {
				Assertions.assertSame(thrown, e.getCause());
			}
		}
		EasyMock.verify(c);
	}

	@Test
	public void testRemove() throws Exception {
		IDfCollection c = null;
		List<IDfTypedObject> l = new ArrayList<>();

		c = EasyMock.createMock(IDfCollection.class);
		for (int i = 0; i < 10; i++) {
			IDfTypedObject o = EasyMock.createMock(IDfTypedObject.class);
			EasyMock.replay(o);
			l.add(o);

			EasyMock.expect(c.next()).andReturn(true).once();
			EasyMock.expect(c.getTypedObject()).andReturn(o).once();
		}
		EasyMock.expect(c.next()).andReturn(false).anyTimes();
		c.close();
		EasyMock.expectLastCall().once();
		EasyMock.replay(c);

		try (DfcCollectionIterator it = new DfcCollectionIterator(c)) {
			int i = 0;
			while (it.hasNext()) {
				IDfTypedObject a = l.get(i++);
				IDfTypedObject b = it.next();
				Assertions.assertSame(a, b);
				Assertions.assertThrows(UnsupportedOperationException.class, () -> it.remove());
			}
			Assertions.assertFalse(it.hasNext());
			Assertions.assertThrows(NoSuchElementException.class, () -> it.next());
		}
		EasyMock.verify(c);
	}

}
