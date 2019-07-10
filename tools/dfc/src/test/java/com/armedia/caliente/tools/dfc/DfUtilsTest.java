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
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class DfUtilsTest {

	@Test
	public void testRunRetryableSimple() throws DfException {
		DfException ret = null;
		IDfSession session = null;
		final IDfLocalTransaction localTx = EasyMock.createMock(IDfLocalTransaction.class);
		final AtomicBoolean opCalled = new AtomicBoolean(false);

		// First, try short-circuits
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.replay(session);
		ret = DfcUtils.runRetryable(session, false, null, null);
		EasyMock.verify(session);

		session = EasyMock.createMock(IDfSession.class);
		EasyMock.replay(session);
		ret = DfcUtils.runRetryable(session, true, null, null);
		EasyMock.verify(session);

		try {
			ret = DfcUtils.runRetryable(null, false, (s) -> {
			}, null);
			Assertions.fail("Did not fail with a null session");
		} catch (NullPointerException e) {
		}
		try {
			ret = DfcUtils.runRetryable(null, true, (s) -> {
			}, null);
			Assertions.fail("Did not fail with a null session");
		} catch (NullPointerException e) {
		}

		// Simple, no TX
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false);
		EasyMock.replay(session);
		opCalled.set(false);
		// Do nothing, no errors
		ret = DfcUtils.runRetryable(session, false, (s) -> opCalled.set(true), null);
		Assertions.assertNull(ret);
		Assertions.assertTrue(opCalled.get());
		EasyMock.verify(session);

		// Simple, new TX with no TX active
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false).once();
		session.beginTrans();
		EasyMock.expectLastCall().once();
		session.commitTrans();
		EasyMock.expectLastCall().once();
		EasyMock.replay(session);
		opCalled.set(false);
		// Do nothing, no errors
		ret = DfcUtils.runRetryable(session, true, (s) -> opCalled.set(true), null);
		Assertions.assertNull(ret);
		Assertions.assertTrue(opCalled.get());
		EasyMock.verify(session);

		// Simple, new TX with TX active
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(true).times(2);
		EasyMock.expect(session.beginTransEx()).andReturn(localTx);
		session.commitTransEx(localTx);
		EasyMock.expectLastCall().once();
		EasyMock.replay(session);
		opCalled.set(false);
		// Do nothing, no errors
		ret = DfcUtils.runRetryable(session, false, (s) -> opCalled.set(true), null);
		Assertions.assertNull(ret);
		Assertions.assertTrue(opCalled.get());
		EasyMock.verify(session);

		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(true).once();
		EasyMock.expect(session.beginTransEx()).andReturn(localTx);
		session.commitTransEx(localTx);
		EasyMock.expectLastCall().once();
		EasyMock.replay(session);
		opCalled.set(false);
		// Do nothing, no errors
		ret = DfcUtils.runRetryable(session, true, (s) -> opCalled.set(true), null);
		Assertions.assertNull(ret);
		Assertions.assertTrue(opCalled.get());
		EasyMock.verify(session);
	}

	@Test
	public void testRunRetryableException() throws DfException {
		final DfException expected = new DfException(UUID.randomUUID().toString());
		DfException ret = null;
		IDfSession session = null;
		final IDfLocalTransaction localTx = EasyMock.createMock(IDfLocalTransaction.class);
		final AtomicBoolean opCalled = new AtomicBoolean(false);

		// Simple, no TX
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false);
		EasyMock.replay(session);
		opCalled.set(false);
		ret = DfcUtils.runRetryable(session, false, (s) -> {
			// Do nothing, no errors
			opCalled.set(true);
			throw expected;
		}, null);
		Assertions.assertNotNull(ret);
		Assertions.assertTrue(opCalled.get());
		Assertions.assertSame(expected, ret);
		EasyMock.verify(session);

		// Simple, new TX with no TX active
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false).once();
		session.beginTrans();
		EasyMock.expectLastCall().once();
		session.abortTrans();
		EasyMock.expectLastCall().once();
		EasyMock.replay(session);
		opCalled.set(false);
		ret = DfcUtils.runRetryable(session, true, (s) -> {
			// Do nothing, no errors
			opCalled.set(true);
			throw expected;
		}, null);
		Assertions.assertNotNull(ret);
		Assertions.assertTrue(opCalled.get());
		Assertions.assertSame(expected, ret);
		EasyMock.verify(session);

		// Simple, new TX with TX active
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(true).times(2);
		EasyMock.expect(session.beginTransEx()).andReturn(localTx);
		session.abortTransEx(localTx);
		EasyMock.expectLastCall().once();
		EasyMock.replay(session);
		opCalled.set(false);
		ret = DfcUtils.runRetryable(session, false, (s) -> {
			// Do nothing, no errors
			opCalled.set(true);
			throw expected;
		}, null);
		Assertions.assertNotNull(ret);
		Assertions.assertTrue(opCalled.get());
		Assertions.assertSame(expected, ret);
		EasyMock.verify(session);

		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(true).once();
		EasyMock.expect(session.beginTransEx()).andReturn(localTx);
		session.abortTransEx(localTx);
		EasyMock.expectLastCall().once();
		EasyMock.replay(session);
		opCalled.set(false);
		ret = DfcUtils.runRetryable(session, true, (s) -> {
			// Do nothing, no errors
			opCalled.set(true);
			throw expected;
		}, null);
		Assertions.assertNotNull(ret);
		Assertions.assertTrue(opCalled.get());
		Assertions.assertSame(expected, ret);
		EasyMock.verify(session);
	}

	@Test
	public void testRunRetryableError() throws DfException {
		final DfException expected = new DfException(UUID.randomUUID().toString());
		IDfSession session = null;
		final IDfLocalTransaction localTx = EasyMock.createMock(IDfLocalTransaction.class);
		final AtomicBoolean opCalled = new AtomicBoolean(false);

		// Simple, no TX
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andThrow(expected);
		EasyMock.replay(session);
		opCalled.set(false);
		try {
			DfcUtils.runRetryable(session, false, (s) -> {
				// Do nothing, no errors
				opCalled.set(true);
				throw new DfException();
			}, null);
			Assertions.fail("Did not raise an unfiltered exception");
		} catch (DfException e) {
			Assertions.assertSame(expected, e);
		}
		Assertions.assertFalse(opCalled.get());
		EasyMock.verify(session);

		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false).once();
		session.beginTrans();
		EasyMock.expectLastCall().andThrow(expected).once();
		EasyMock.replay(session);
		opCalled.set(false);
		try {
			DfcUtils.runRetryable(session, true, (s) -> {
				// Do nothing, no errors
				opCalled.set(true);
				throw new DfException();
			}, null);
			Assertions.fail("Did not raise an unfiltered exception");
		} catch (DfException e) {
			Assertions.assertSame(expected, e);
		}
		Assertions.assertFalse(opCalled.get());
		EasyMock.verify(session);

		// Simple, new TX with no TX active
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false).once();
		session.beginTrans();
		EasyMock.expectLastCall().andThrow(expected).once();
		EasyMock.replay(session);
		opCalled.set(false);
		try {
			DfcUtils.runRetryable(session, true, (s) -> {
				// Do nothing, no errors
				opCalled.set(true);
				throw new DfException();
			}, null);
			Assertions.fail("Did not raise an unfiltered exception");
		} catch (DfException e) {
			Assertions.assertSame(expected, e);
		}
		Assertions.assertFalse(opCalled.get());
		EasyMock.verify(session);

		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false).once();
		session.beginTrans();
		EasyMock.expectLastCall().once();
		session.abortTrans();
		EasyMock.expectLastCall().andThrow(expected).once();
		EasyMock.replay(session);
		opCalled.set(false);
		try {
			DfcUtils.runRetryable(session, true, (s) -> {
				// Do nothing, no errors
				opCalled.set(true);
				throw new DfException();
			}, null);
			Assertions.fail("Did not raise an unfiltered exception");
		} catch (DfException e) {
			Assertions.assertSame(expected, e);
		}
		Assertions.assertTrue(opCalled.get());
		EasyMock.verify(session);

		// Simple, new TX with TX active
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(true).times(2);
		EasyMock.expect(session.beginTransEx()).andThrow(expected).once();
		EasyMock.replay(session);
		opCalled.set(false);
		try {
			DfcUtils.runRetryable(session, false, (s) -> {
				// Do nothing, no errors
				opCalled.set(true);
				throw new DfException();
			}, null);
			Assertions.fail("Did not raise an unfiltered exception");
		} catch (DfException e) {
			Assertions.assertSame(expected, e);
		}
		Assertions.assertFalse(opCalled.get());
		EasyMock.verify(session);

		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(true).once();
		EasyMock.expect(session.beginTransEx()).andReturn(localTx).once();
		session.abortTransEx(localTx);
		EasyMock.expectLastCall().andThrow(expected).once();
		EasyMock.replay(session);
		opCalled.set(false);
		try {
			DfcUtils.runRetryable(session, true, (s) -> {
				// Do nothing, no errors
				opCalled.set(true);
				throw new DfException();
			}, null);
			Assertions.fail("Did not raise an unfiltered exception");
		} catch (DfException e) {
			Assertions.assertSame(expected, e);
		}
		Assertions.assertTrue(opCalled.get());
		EasyMock.verify(session);
	}

	@Test
	public void testRunRetryableFilter() throws DfException {
		final String uuid = UUID.randomUUID().toString();
		final DfException expected = new DfException(uuid);
		final IDfLocalTransaction localTx = EasyMock.createMock(IDfLocalTransaction.class);
		DfException ret = null;
		IDfSession session = null;
		final AtomicBoolean opCalled = new AtomicBoolean(false);

		final Predicate<DfException> filter = (e) -> (e != null) && StringUtils.equalsIgnoreCase(uuid, e.getMessage());

		// Simple, no TX
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false).once();
		EasyMock.replay(session);
		opCalled.set(false);
		ret = DfcUtils.runRetryable(session, false, (s) -> {
			// Do nothing, no errors
			opCalled.set(true);
			throw expected;
		}, filter);
		Assertions.assertTrue(opCalled.get());
		Assertions.assertSame(expected, ret);
		EasyMock.verify(session);

		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(false).once();
		EasyMock.replay(session);
		opCalled.set(false);
		try {
			ret = DfcUtils.runRetryable(session, false, (s) -> {
				// Do nothing, no errors
				opCalled.set(true);
				throw new DfException();
			}, filter);
			Assertions.fail("Did not raise an unmatched exception");
		} catch (DfException e) {
			Assertions.assertNotSame(expected, e);
		}
		Assertions.assertTrue(opCalled.get());
		Assertions.assertSame(expected, ret);
		EasyMock.verify(session);

		// Nested TX
		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(true).times(2);
		EasyMock.expect(session.beginTransEx()).andReturn(localTx).once();
		session.abortTransEx(localTx);
		EasyMock.expectLastCall().once();
		EasyMock.replay(session);
		opCalled.set(false);
		ret = DfcUtils.runRetryable(session, false, (s) -> {
			// Do nothing, no errors
			opCalled.set(true);
			throw expected;
		}, filter);
		Assertions.assertTrue(opCalled.get());
		Assertions.assertSame(expected, ret);
		EasyMock.verify(session);

		session = EasyMock.createMock(IDfSession.class);
		EasyMock.expect(session.isTransactionActive()).andReturn(true).times(2);
		EasyMock.expect(session.beginTransEx()).andReturn(localTx).once();
		session.abortTransEx(localTx);
		EasyMock.expectLastCall().once();
		EasyMock.replay(session);
		opCalled.set(false);
		try {
			ret = DfcUtils.runRetryable(session, false, (s) -> {
				// Do nothing, no errors
				opCalled.set(true);
				throw new DfException();
			}, filter);
			Assertions.fail("Did not raise an unmatched exception");
		} catch (DfException e) {
			Assertions.assertNotSame(expected, e);
		}
		Assertions.assertTrue(opCalled.get());
		Assertions.assertSame(expected, ret);
		EasyMock.verify(session);
	}

	@Test
	public void testExtendedPermissions() throws Exception {
		Collection<Pair<String[], Integer>> options = new ArrayList<>();
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR
		}, 0b00000000000000000000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR
		}, 0b00000000000000000000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR
		}, 0b00000000000000000000000000000010));
		options.add(Pair.of(new String[] {}, 0b00000000000000000000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR
		}, 0b00000000000000010000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR
		}, 0b00000000000000010000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR
		}, 0b00000000000000010000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR
		}, 0b00000000000000010000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR
		}, 0b00000000000000100000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR
		}, 0b00000000000000100000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR
		}, 0b00000000000000100000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR
		}, 0b00000000000000100000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR
		}, 0b00000000000000110000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR
		}, 0b00000000000000110000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR
		}, 0b00000000000000110000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR
		}, 0b00000000000000110000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001000000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001000000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001000000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001000000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001010000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001010000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001010000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001010000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001100000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001100000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001100000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001100000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001110000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001110000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001110000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR
		}, 0b00000000000001110000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010000000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010000000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010000000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010000000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010010000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010010000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010010000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010010000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010100000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010100000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010100000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010100000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010110000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010110000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010110000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000010110000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011000000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011000000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011000000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011000000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011010000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011010000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011010000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011010000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011100000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011100000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011100000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011100000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011110000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011110000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011110000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR
		}, 0b00000000000011110000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100000000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100000000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100000000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100000000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100010000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100010000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100010000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100010000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100100000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100100000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100100000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100100000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100110000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100110000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100110000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000100110000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101000000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101000000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101000000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101000000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101010000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101010000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101010000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101010000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101100000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101100000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101100000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101100000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101110000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101110000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101110000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000101110000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110000000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110000000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110000000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110000000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110010000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110010000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110010000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110010000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110100000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110100000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110100000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110100000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110110000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110110000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110110000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000110110000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111000000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111000000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111000000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111000000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111010000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111010000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111010000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111010000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111100000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111100000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111100000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111100000000000000011));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111110000000000000000));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111110000000000000001));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111110000000000000010));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b00000000000111110000000000000011));

		final Set<String> expectedSet = new TreeSet<>();
		options.forEach((p) -> {
			expectedSet.clear();
			for (String x : p.getLeft()) {
				expectedSet.add(StringUtils.upperCase(x));
			}

			int expectedInt = p.getRight();

			Set<String> actualSet = new TreeSet<>(DfcUtils.decodeExtendedPermission(expectedInt));
			int actualInt = DfcUtils.decodeExtendedPermission(expectedSet);
			Assertions.assertEquals(expectedSet, actualSet,
				String.format("Failed testing %s (%s)", expectedSet, Integer.toBinaryString(expectedInt)));
			Assertions.assertEquals(expectedInt, actualInt, String.format("Failed testing %s (%s vs. %s)", expectedSet,
				Integer.toBinaryString(expectedInt), Integer.toBinaryString(actualInt)));
		});

		// Extreme cases
		options.clear();
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR,
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b11111111111111111111111111111100));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR,
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b11111111111111111111111111111101));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_EXECUTE_PROC_STR, IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR,
			IDfACL.DF_XPERMIT_CHANGE_OWNER_STR, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR,
			IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b11111111111111111111111111111110));
		options.add(Pair.of(new String[] {
			IDfACL.DF_XPERMIT_CHANGE_STATE_STR, IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, IDfACL.DF_XPERMIT_CHANGE_OWNER_STR,
			IDfACL.DF_XPERMIT_DELETE_OBJECT_STR, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR
		}, 0b11111111111111111111111111111111));
		options.forEach((p) -> {
			expectedSet.clear();
			for (String x : p.getLeft()) {
				expectedSet.add(StringUtils.upperCase(x));
			}

			int expectedInt = p.getRight();

			Set<String> actualSet = new TreeSet<>(DfcUtils.decodeExtendedPermission(expectedInt));
			Assertions.assertEquals(expectedSet, actualSet,
				String.format("Failed testing %s (%s)", expectedSet, Integer.toBinaryString(expectedInt)));
		});
	}
}
