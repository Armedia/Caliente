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
 * @author diego
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
		return getWrapped().isTransactionActive();
	}

	@Override
	protected boolean beginTransaction() throws Exception {
		IDfSession s = getWrapped();
		if (s.isTransactionActive()) {
			throw new Exception("Attempting to start a top-level transaction, but nesting is already in place");
		}
		s.beginTrans();
		return true;
	}

	@Override
	protected boolean beginNestedTransaction() throws Exception {
		IDfSession s = getWrapped();
		if (!s.isTransactionActive()) {
			throw new Exception("Attempting to start a nested transaction, but no enclosing transaction is in place");
		}
		this.localTx.push(s.beginTransEx());
		return true;
	}

	@Override
	protected void commitTransaction() throws Exception {
		IDfSession s = getWrapped();
		if (this.localTx.isEmpty()) {
			s.commitTrans();
		} else {
			s.commitTransEx(this.localTx.pop());
		}
	}

	@Override
	protected void rollbackTransaction() throws Exception {
		IDfSession s = getWrapped();
		if (this.localTx.isEmpty()) {
			s.abortTrans();
		} else {
			s.abortTransEx(this.localTx.pop());
		}
	}
}