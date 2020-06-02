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
package com.armedia.caliente.engine.alfresco.bi;

import java.util.concurrent.atomic.AtomicLong;

import com.armedia.caliente.engine.common.SessionWrapper;

public class AlfSessionWrapper extends SessionWrapper<AlfRoot> {

	private static final AtomicLong ID_COUNTER = new AtomicLong(0);

	private final String id;

	protected AlfSessionWrapper(AlfSessionFactory factory, AlfRoot wrapped) {
		super(factory, wrapped);
		this.id = String.format("%16X", AlfSessionWrapper.ID_COUNTER.getAndIncrement());
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	protected boolean isSupportsTransactions() {
		return false;
	}

	@Override
	protected boolean isSupportsNestedTransactions() {
		return false;
	}

	@Override
	protected boolean isTransactionActive() throws Exception {
		return false;
	}

	@Override
	protected boolean beginTransaction() throws Exception {
		return false;
	}

	@Override
	protected boolean beginNestedTransaction() throws Exception {
		return false;
	}

	@Override
	protected void commitTransaction() throws Exception {
	}

	@Override
	protected void rollbackTransaction() throws Exception {
	}
}