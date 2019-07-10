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
package com.armedia.caliente.cli.ticketdecoder;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public abstract class ContentFinder implements Callable<Void> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Set<String> scannedIds;
	private final DfcSessionPool pool;
	protected final String source;
	private final Consumer<IDfId> consumer;

	/**
	 * @param pool
	 */
	public ContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source, Consumer<IDfId> consumer) {
		this.scannedIds = scannedIds;
		this.pool = pool;
		this.source = source;
		this.consumer = consumer;
	}

	@Override
	public final Void call() throws DfException {
		final IDfSession session = this.pool.acquireSession();
		final IDfLocalTransaction tx = DfcUtils.openTransaction(session);
		try {
			Stream<IDfId> ids = getIds(session);
			if (ids != null) {
				ids //
					.filter(Objects::nonNull) //
					.filter(IDfId::isObjectId) //
					.filter((id) -> this.scannedIds.add(id.getId())) //
					.forEach(this.consumer) //
				;
			}
			return null;
		} finally {
			try {
				// No matter what...roll back!
				DfcUtils.abortTransaction(session, tx);
			} catch (DfException e) {
				this.log.warn("Could not abort an open transaction", e);
			} finally {
				this.pool.releaseSession(session);
			}
		}
	}

	protected abstract Stream<IDfId> getIds(IDfSession session) throws DfException;

}