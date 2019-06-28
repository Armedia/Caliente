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

package com.armedia.caliente.store.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import com.armedia.caliente.store.CmfOperationException;
import com.armedia.caliente.store.CmfStoreOperation;

/**
 *
 *
 */
class JdbcOperation extends CmfStoreOperation<Connection> {

	private final boolean jtaManaged;

	public JdbcOperation(Connection wrapped, boolean jtaManaged, boolean exclusive) throws CmfOperationException {
		super(wrapped, exclusive);
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
