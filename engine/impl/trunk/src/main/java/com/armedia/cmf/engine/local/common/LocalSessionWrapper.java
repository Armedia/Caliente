package com.armedia.cmf.engine.local.common;

import java.net.URL;

import com.armedia.cmf.engine.SessionWrapper;

public class LocalSessionWrapper extends SessionWrapper<URL> {

	protected LocalSessionWrapper(LocalSessionFactory factory, URL wrapped) {
		super(factory, wrapped);
	}

	@Override
	public String getId() {
		return null;
	}

	@Override
	protected boolean isSupportsTransactions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isSupportsNestedTransactions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isTransactionActive() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean beginTransaction() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean beginNestedTransaction() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void commitTransaction() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void rollbackTransaction() throws Exception {
		// TODO Auto-generated method stub

	}
}