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
package com.armedia.caliente.tools.dfc;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedConsumer;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;

public class DfcQuery extends CloseableIterator<IDfTypedObject> {

	public static enum Type {
		//
		DF_READ_QUERY(IDfQuery.DF_READ_QUERY), //
		DF_QUERY(IDfQuery.DF_QUERY), //
		DF_CACHE_QUERY(IDfQuery.DF_CACHE_QUERY), //
		DF_EXEC_QUERY(IDfQuery.DF_EXEC_QUERY), //
		DF_EXECREAD_QUERY(IDfQuery.DF_EXECREAD_QUERY), //
		DF_APPLY(IDfQuery.DF_APPLY), //
		//
		;

		private final int type;

		private Type(int type) {
			this.type = type;
		}
	}

	public static final int MIN_BATCH_SIZE = 100;
	public static final int DEFAULT_BATCH_SIZE = 1000;
	public static final int MAX_BATCH_SIZE = Integer.MAX_VALUE;
	public static final Type DEFAULT_TYPE = Type.DF_QUERY;

	public static void run(IDfSession session, String dql) throws DfException {
		DfcQuery.run(session, dql, null, 0);
	}

	public static void run(IDfSession session, String dql, Type type) throws DfException {
		DfcQuery.run(session, dql, type, 0);
	}

	public static void run(IDfSession session, String dql, Type type, int batchSize) throws DfException {
		try (DfcQuery q = new DfcQuery(session, dql, type, batchSize)) {
			// Do nothing...
		}
	}

	public static void run(IDfSession session, String dql, CheckedConsumer<IDfTypedObject, DfException> consumer)
		throws DfException {
		DfcQuery.run(session, dql, null, 0, consumer);
	}

	public static void run(IDfSession session, String dql, Type type,
		CheckedConsumer<IDfTypedObject, DfException> consumer) throws DfException {
		DfcQuery.run(session, dql, type, 0, consumer);
	}

	public static void run(IDfSession session, String dql, Type type, int batchSize,
		CheckedConsumer<IDfTypedObject, DfException> consumer) throws DfException {
		Objects.requireNonNull(consumer, "Must provide a valid consumer");
		try (DfcQuery q = new DfcQuery(session, dql, type, batchSize)) {
			q.forEachRemaining(consumer);
		}
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Type type;
	private final String dql;
	private final int batchSize;
	private final IDfSession session;
	private final IDfCollection collection;

	public DfcQuery(IDfSession session, String dql) throws DfException {
		this(session, dql, null, 0);
	}

	public DfcQuery(IDfSession session, String dql, Type type) throws DfException {
		this(session, dql, type, 0);
	}

	public DfcQuery(IDfSession session, String dql, int batchSize) throws DfException {
		this(session, dql, null, batchSize);
	}

	public DfcQuery(IDfSession session, String dql, Type type, int batchSize) throws DfException {
		this.session = Objects.requireNonNull(session, "Must provide a non-null session");
		this.dql = Objects.requireNonNull(dql, "Must provide a DQL query to execute");
		this.type = Tools.coalesce(type, DfcQuery.DEFAULT_TYPE);
		IDfQuery query = new DfClientX().getQuery();
		if (this.log.isTraceEnabled()) {
			this.log.trace("Executing DQL (type={}):{}{}", this.type, Tools.NL, dql);
		}
		query.setDQL(dql);
		if (batchSize > 0) {
			batchSize = Tools.ensureBetween(DfcQuery.MIN_BATCH_SIZE, batchSize, DfcQuery.MAX_BATCH_SIZE);
		} else {
			batchSize = DfcQuery.DEFAULT_BATCH_SIZE;
		}
		this.batchSize = batchSize;
		query.setBatchSize(this.batchSize);
		boolean ok = false;
		try {
			this.collection = query.execute(session, this.type.type);
			ok = true;
		} finally {
			if (!ok) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception raised while executing the query:{}{}", Tools.NL, dql);
				}
			}
		}
	}

	private static DfcQuery restart(DfcQuery query) throws DfException {
		Objects.requireNonNull(query, "Must provide a query to restart");
		return new DfcQuery(query.getSession(), query.getDql(), query.getType(), query.getBatchSize());
	}

	public <EX extends Exception> void forEachRemaining(CheckedConsumer<? super IDfTypedObject, EX> consumer)
		throws DfException, EX {
		Objects.requireNonNull(consumer, "Must provide a non-null consumer");
		while (hasNext()) {
			consumer.acceptChecked(next());
		}
	}

	public DfcQuery restart() throws DfException {
		return DfcQuery.restart(this);
	}

	public Type getType() {
		return this.type;
	}

	public String getDql() {
		return this.dql;
	}

	public int getBatchSize() {
		return this.batchSize;
	}

	public IDfSession getSession() {
		return this.session;
	}

	public IDfTypedObject current() {
		return this.collection;
	}

	@Override
	protected Result findNext() throws DfException {
		if (!this.collection.next()) { return null; }
		return found(this.collection.getTypedObject());
	}

	@Override
	protected void doClose() throws DfException {
		this.collection.close();
	}

}