package com.armedia.caliente.tools.dfc;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.dfc.DfcCollectionIterator;
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
