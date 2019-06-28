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
package com.armedia.caliente.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CmfStoreOperation<CONNECTION> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final CONNECTION connection;
	private final boolean exclusive;
	private boolean valid = true;
	private boolean transactionOpen = false;

	protected CmfStoreOperation(CONNECTION wrapped, boolean exclusive) {
		if (wrapped == null) {
			throw new IllegalArgumentException("Must provide the connection that this operation is related to");
		}
		this.connection = wrapped;
		this.exclusive = exclusive;
	}

	protected final void assertValid() {
		if (!this.valid) { throw new IllegalStateException("This session is no longer valid"); }
	}

	protected abstract boolean supportsTransactions();

	protected final boolean isExclusive() {
		return this.exclusive;
	}

	public final CONNECTION getConnection() {
		assertValid();
		return this.connection;
	}

	public final boolean begin() throws CmfOperationException {
		assertValid();
		if (this.transactionOpen) { return false; }
		boolean tx = false;
		if (supportsTransactions()) {
			tx = beginTransaction();
		}
		this.transactionOpen = tx;
		return tx;
	}

	protected abstract boolean beginTransaction() throws CmfOperationException;

	public final void commit() throws CmfOperationException {
		assertValid();
		if (!this.transactionOpen) { return; }
		if (!supportsTransactions()) {
			throw new UnsupportedOperationException("This type of session doesn't support transactions");
		}
		try {
			commitTransaction();
		} finally {
			this.transactionOpen = false;
		}
	}

	protected abstract void commitTransaction() throws CmfOperationException;

	public final void rollback() throws CmfOperationException {
		assertValid();
		if (!this.transactionOpen) { return; }
		if (!supportsTransactions()) {
			throw new UnsupportedOperationException("This type of session doesn't support transactions");
		}
		try {
			rollbackTransaction();
		} finally {
			this.transactionOpen = false;
		}
	}

	protected abstract void rollbackTransaction() throws CmfOperationException;

	public final void close() throws CmfStorageException {
		if (!this.valid) { return; }
		try {
			closeConnection();
		} catch (Exception e) {
			throw new CmfStorageException("Failed to close the connection", e);
		} finally {
			this.valid = false;
		}
	}

	protected final void closeQuietly() {
		try {
			close();
		} catch (CmfStorageException e) {
			// Ignore it...
			if (this.log.isTraceEnabled()) {
				this.log.error("Exception raised while closing an operation", e);
			}
		}
	}

	protected abstract void closeConnection() throws Exception;
}