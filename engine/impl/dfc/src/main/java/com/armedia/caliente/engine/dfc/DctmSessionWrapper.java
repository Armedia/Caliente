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
/**
 *
 */

package com.armedia.caliente.engine.dfc;

import java.util.Stack;

import com.armedia.caliente.engine.SessionFactoryException;
import com.armedia.caliente.engine.SessionWrapper;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 *
 *
 */
public class DctmSessionWrapper extends SessionWrapper<IDfSession> {

	private Stack<IDfLocalTransaction> localTx = new Stack<>();
	private final String id;

	protected DctmSessionWrapper(DctmSessionFactory factory, IDfSession wrapped) throws SessionFactoryException {
		super(factory, wrapped);
		try {
			this.id = wrapped.getSessionId();
		} catch (DfException e) {
			throw new SessionFactoryException("Failed to get the session ID", e);
		}
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	protected boolean isSupportsTransactions() {
		return true;
	}

	@Override
	protected boolean isSupportsNestedTransactions() {
		return true;
	}

	@Override
	protected boolean isTransactionActive() throws Exception {
		return get().isTransactionActive();
	}

	@Override
	protected boolean beginTransaction() throws Exception {
		IDfSession s = get();
		if (s.isTransactionActive()) {
			throw new Exception("Attempting to start a top-level transaction, but nesting is already in place");
		}
		s.beginTrans();
		return true;
	}

	@Override
	protected boolean beginNestedTransaction() throws Exception {
		IDfSession s = get();
		if (!s.isTransactionActive()) {
			throw new Exception("Attempting to start a nested transaction, but no enclosing transaction is in place");
		}
		this.localTx.push(s.beginTransEx());
		return true;
	}

	@Override
	protected void commitTransaction() throws Exception {
		IDfSession s = get();
		if (this.localTx.isEmpty()) {
			s.commitTrans();
		} else {
			s.commitTransEx(this.localTx.pop());
		}
	}

	@Override
	protected void rollbackTransaction() throws Exception {
		IDfSession s = get();
		if (this.localTx.isEmpty()) {
			s.abortTrans();
		} else {
			s.abortTransEx(this.localTx.pop());
		}
	}
}