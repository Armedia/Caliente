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
package com.armedia.caliente.cli.typedumper;

import java.util.Objects;
import java.util.function.Predicate;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.function.CheckedConsumer;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;

public class ExtractorLogic implements PooledWorkersLogic<IDfSession, String, Exception> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcSessionPool pool;
	private final Predicate<String> typeFilter;
	private final CheckedConsumer<IDfType, DfException> typeConsumer;

	public ExtractorLogic(DfcSessionPool pool, CheckedConsumer<IDfType, DfException> typeConsumer,
		Predicate<String> typeFilter) throws ScriptException {
		this.pool = Objects.requireNonNull(pool, "Must provide a DfcSessionPool");
		this.typeConsumer = Objects.requireNonNull(typeConsumer, "Must provide a type Consumer");
		this.typeFilter = typeFilter;
	}

	@Override
	public IDfSession initialize() throws DfException {
		return this.pool.acquireSession();
	}

	@Override
	public void process(IDfSession session, String typeName) throws Exception {
		if (typeName == null) { return; }
		final IDfLocalTransaction tx = DfcUtils.openTransaction(session);
		try {
			if ((this.typeFilter == null) || !this.typeFilter.test(typeName)) {
				IDfType type = session.getType(typeName);
				if (type == null) { return; }
				this.typeConsumer.acceptChecked(type);
			}
		} finally {
			try {
				// No matter what...roll back!
				DfcUtils.abortTransaction(session, tx);
			} catch (DfException e) {
				this.log.warn("Could not abort an open transaction", e);
			}
		}
	}

	@Override
	public void handleFailure(IDfSession session, String typeName, Exception raised) {
		this.log.error("Failed to retrieve the type data for [{}]", typeName, raised);
	}

	@Override
	public void cleanup(IDfSession session) {
		this.pool.releaseSession(session);
	}
}