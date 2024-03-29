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
package com.armedia.caliente.store.local;

import java.io.File;

import com.armedia.caliente.store.CmfOperationException;
import com.armedia.caliente.store.CmfStoreOperation;

public class LocalContentStoreOperation extends CmfStoreOperation<File> {

	public LocalContentStoreOperation(File wrapped, boolean exclusive) {
		super(wrapped, exclusive);
	}

	@Override
	protected boolean supportsTransactions() {
		return false;
	}

	@Override
	protected boolean beginTransaction() throws CmfOperationException {
		return false;
	}

	@Override
	protected void commitTransaction() throws CmfOperationException {
	}

	@Override
	protected void rollbackTransaction() throws CmfOperationException {
	}

	@Override
	protected void closeConnection() throws Exception {
	}
}