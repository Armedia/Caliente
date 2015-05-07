package com.armedia.cmf.engine.local.common;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import com.armedia.cmf.engine.SessionWrapper;

public class LocalSessionWrapper extends SessionWrapper<File> {

	private static final AtomicLong ID_COUNTER = new AtomicLong(0);

	private final String id;

	protected LocalSessionWrapper(LocalSessionFactory factory, File wrapped) {
		super(factory, wrapped);
		this.id = String.format("%16X", LocalSessionWrapper.ID_COUNTER.getAndIncrement());
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