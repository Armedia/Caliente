package com.armedia.caliente.engine.ucm;

import java.util.concurrent.atomic.AtomicLong;

import com.armedia.caliente.engine.SessionWrapper;

public class UcmSessionWrapper extends SessionWrapper<UcmSession> {

	private static final AtomicLong ID = new AtomicLong(0);

	private final String id;

	public UcmSessionWrapper(UcmSessionFactory factory, UcmSession session) {
		super(factory, session);
		this.id = String.format("{%08X}", UcmSessionWrapper.ID.getAndIncrement());
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