/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.util.concurrent.atomic.AtomicLong;

import com.armedia.cmf.engine.SessionWrapper;

/**
 * @author diego
 *
 */
public class ShptSessionWrapper extends SessionWrapper<ShptSession> {

	private static final AtomicLong SESSION_ID = new AtomicLong(0);

	private final String id;

	protected ShptSessionWrapper(ShptSessionFactory factory, ShptSession wrapped) {
		super(factory, wrapped);
		this.id = String.format("%08x", ShptSessionWrapper.SESSION_ID.incrementAndGet());
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	protected boolean isSupportsTransactions() {
		return false;
	}

	@Override
	protected boolean isSupportsNestedTransactions() {
		return false;
	}

	@Override
	protected boolean isTransactionActive() throws Exception {
		return false;
	}

	@Override
	protected boolean beginTransaction() throws Exception {
		return false;
	}

	@Override
	protected boolean beginNestedTransaction() throws Exception {
		return false;
	}

	@Override
	protected void commitTransaction() throws Exception {
	}

	@Override
	protected void rollbackTransaction() throws Exception {
	}
}