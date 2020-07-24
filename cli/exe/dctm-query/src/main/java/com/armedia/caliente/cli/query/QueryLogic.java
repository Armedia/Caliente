/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.cli.query;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedSupplier;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

public class QueryLogic implements Callable<Void> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcSessionPool pool;
	private final String dql;
	private final CheckedSupplier<ResultsPersistor, Exception> persistor;
	private final File file;
	private final int batchSize;
	private final Logger status;

	public QueryLogic(Logger status, DfcSessionPool pool, CheckedSupplier<ResultsPersistor, Exception> persistor,
		String dql, File file, int batchSize) throws ScriptException {
		this.status = status;
		this.pool = Objects.requireNonNull(pool, "Must provide a DfcSessionPool");
		this.dql = dql;
		this.persistor = persistor;
		this.file = file;
		this.batchSize = Math.max(100, batchSize);
	}

	private void log(String message, Object... args) {
		if (this.status != null) {
			this.status.info(message, args);
		}
	}

	private Map<String, IDfAttr> getAttributes(DfcQuery query) throws DfException {
		IDfTypedObject c = query.current();
		int atts = c.getAttrCount();
		log("Introspecting {} attributes...", atts);
		Map<String, IDfAttr> ret = new LinkedHashMap<>();
		for (int i = 0; i < atts; i++) {
			IDfAttr att = c.getAttr(i);
			ret.put(att.getName(), att);
		}
		log("Found the following {} attributes: {}", atts, ret.keySet());
		return Tools.freezeMap(ret);
	}

	@Override
	public Void call() throws Exception {
		log("Acquiring the DFC session");
		final IDfSession session = this.pool.acquireSession();
		final IDfLocalTransaction tx;
		try {
			log("Opening a transaction for safety");
			tx = DfcUtils.openTransaction(session);
			log("Running the DQL Query (batch size = {})...", this.batchSize);
			try (DfcQuery query = new DfcQuery(session, this.dql, this.batchSize)) {
				log("Query open, persisting results");
				try (ResultsPersistor p = this.persistor.getChecked()) {
					log("Initializing the results at [{}] ...");
					p.initialize(this.file, this.dql, getAttributes(query).values());
					final AtomicLong counter = new AtomicLong();
					log("Results file ready, writing out the records");
					query.forEachRemaining((o) -> {
						p.persist(o);
						final long c = counter.incrementAndGet();
						if ((c % 1000) == 0) {
							log("Persisted {} results...", c);
						}
					});
					log("Persistence complete: {} results persisted", counter.get());
					return null;
				}
			} finally {
				try {
					// No matter what...roll back!
					log("Aborting the transaction");
					DfcUtils.abortTransaction(session, tx);
				} catch (DfException e) {
					this.log.warn("Could not abort an open transaction", e);
				}
			}
		} finally {
			log("Releasing the DFC session");
			this.pool.releaseSession(session);
		}
	}
}