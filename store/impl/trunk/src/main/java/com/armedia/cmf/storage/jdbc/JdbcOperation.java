/**
 *
 */

package com.armedia.cmf.storage.jdbc;

import java.sql.Connection;

import com.armedia.cmf.storage.CmfStoreOperation;

/**
 * @author diego
 *
 */
class JdbcOperation extends CmfStoreOperation<Connection> {

	private final boolean jtaManaged;

	public JdbcOperation(Connection wrapped, boolean jtaManaged) {
		super(wrapped);
		this.jtaManaged = jtaManaged;
	}

	@Override
	protected boolean supportsTransactions() {
		return true;
	}

	@Override
	protected boolean beginTransaction() throws Exception {
		if (this.jtaManaged) { return true; }
		getConnection().setAutoCommit(false);
		return true;
	}

	@Override
	protected void commitTransaction() throws Exception {
		getConnection().commit();
	}

	@Override
	protected void rollbackTransaction() throws Exception {
		getConnection().rollback();
	}

	@Override
	protected void closeConnection() throws Exception {
		getConnection().close();
	}
}
