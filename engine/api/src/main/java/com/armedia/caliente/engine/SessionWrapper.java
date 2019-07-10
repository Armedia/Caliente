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
package com.armedia.caliente.engine;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SessionWrapper<SESSION> implements Supplier<SESSION>, AutoCloseable {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final SessionFactory<SESSION> factory;
	private final SESSION wrapped;
	private boolean valid = true;

	private int openCount = 0;

	protected SessionWrapper(SessionFactory<SESSION> factory, SESSION wrapped) {
		this.factory = factory;
		this.wrapped = wrapped;
	}

	protected final void assertValid() {
		if (!this.valid) { throw new IllegalStateException("This session is no longer valid"); }
	}

	public abstract String getId();

	protected abstract boolean isSupportsTransactions();

	protected abstract boolean isSupportsNestedTransactions();

	@Override
	public final SESSION get() {
		assertValid();
		return this.wrapped;
	}

	public final boolean isTransactionOpen() {
		return (this.openCount > 0);
	}

	public final boolean isNestedTransactionOpen() {
		return (this.openCount > 1);
	}

	public final int getTransactionNestingDepth() {
		return this.openCount;
	}

	public final boolean begin() {
		assertValid();
		if (this.openCount > 0) {
			if (!isSupportsNestedTransactions()) { return false; }
		}
		boolean tx = false;
		if (isSupportsTransactions()) {
			try {
				if (this.openCount == 0) {
					tx = beginTransaction();
				} else {
					tx = beginNestedTransaction();
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to start a new transaction", e);
			}
		}
		this.openCount += (tx ? 1 : 0);
		return tx;
	}

	private void end(boolean commit) {
		assertValid();
		if (this.openCount < 1) { return; }
		if (!isSupportsTransactions()) {
			throw new UnsupportedOperationException("This type of session doesn't support transactions");
		}
		try {
			if (commit) {
				commitTransaction();
			} else {
				rollbackTransaction();
			}
		} catch (Exception e) {
			throw new RuntimeException(
				String.format("Failed to %s the current transaction", (commit ? "commit" : "roll back")), e);
		} finally {
			this.openCount--;
		}
	}

	public final void commit() {
		end(true);
	}

	public final void rollback() {
		end(false);
	}

	@Override
	public final void close() {
		close(false);
	}

	public final void close(boolean commit) {
		if (!this.valid) { return; }
		while (this.openCount > 0) {
			end(commit);
		}
		try {
			this.factory.releaseSession(this.wrapped);
		} finally {
			this.valid = false;
		}
	}

	protected abstract boolean isTransactionActive() throws Exception;

	protected abstract boolean beginTransaction() throws Exception;

	protected abstract boolean beginNestedTransaction() throws Exception;

	protected abstract void commitTransaction() throws Exception;

	protected abstract void rollbackTransaction() throws Exception;
}