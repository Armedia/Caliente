package com.armedia.caliente.engine.xds;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.SessionWrapper;

public class CmisSessionWrapper extends SessionWrapper<Session> {

	private static final AtomicLong ID = new AtomicLong(0);

	private final String id;

	public CmisSessionWrapper(CmisSessionFactory factory, Session session) {
		super(factory, session);
		this.id = String.format("{%08X}", CmisSessionWrapper.ID.getAndIncrement());
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