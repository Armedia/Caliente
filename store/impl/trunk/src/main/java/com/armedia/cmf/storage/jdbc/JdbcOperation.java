/**
 *
 */

package com.armedia.cmf.storage.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import com.armedia.cmf.storage.CmfOperationException;
import com.armedia.cmf.storage.CmfStoreOperation;

/**
 * @author diego
 *
 */
class JdbcOperation extends CmfStoreOperation<Connection> {

	private final boolean jtaManaged;

	public JdbcOperation(Connection wrapped, boolean jtaManaged) throws CmfOperationException {
		super(wrapped);
		this.jtaManaged = jtaManaged;
		try {
			if (!jtaManaged && !wrapped.getAutoCommit()) {
				wrapped.setAutoCommit(true);
			}
		} catch (SQLException e) {
			throw new CmfOperationException("Failed to set the default autocommit state to true", e);
		}
	}

	protected void restoreAutoCommit(Connection c) {
		try {
			c.setAutoCommit(true);
		} catch (SQLException e) {
			if (this.log.isDebugEnabled()) {
				this.log.warn("Failed to re-enable autocommit", e);
			}
		}
	}

	@Override
	protected boolean supportsTransactions() {
		return true;
	}

	@Override
	protected boolean beginTransaction() throws CmfOperationException {
		if (this.jtaManaged) { return true; }
		try {
			getConnection().setAutoCommit(false);
		} catch (SQLException e) {
			throw new CmfOperationException("Failed to begin a new transaction", e);
		}
		return true;
	}

	@Override
	protected void commitTransaction() throws CmfOperationException {
		if (this.jtaManaged) { return; }
		final Connection connection = getConnection();
		boolean ok = false;
		try {
			connection.commit();
			ok = true;
		} catch (SQLException e) {
			throw new CmfOperationException("Failed to commit the active transaction", e);
		} finally {
			if (ok) {
				restoreAutoCommit(connection);
			}
		}
	}

	@Override
	protected void rollbackTransaction() throws CmfOperationException {
		if (this.jtaManaged) { return; }
		final Connection connection = getConnection();
		boolean ok = false;
		try {
			connection.rollback();
			ok = true;
		} catch (SQLException e) {
			throw new CmfOperationException("Failed to rollback the active transaction", e);
		} finally {
			if (ok) {
				restoreAutoCommit(connection);
			}
		}
	}

	@Override
	protected void closeConnection() throws Exception {
		getConnection().close();
	}
}
