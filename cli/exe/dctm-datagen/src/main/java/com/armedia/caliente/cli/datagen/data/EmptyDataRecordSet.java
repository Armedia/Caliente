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
package com.armedia.caliente.cli.datagen.data;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public final class EmptyDataRecordSet<D extends Object, R extends Object, S extends Object>
	extends DataRecordSet<D, R, S> {

	private static final Map<String, Integer> COLUMNS = Collections.singletonMap("COLUMN", 0);

	public EmptyDataRecordSet() throws Exception {
		super(false, 0, null);
	}

	@Override
	protected D initData() {
		return null;
	}

	@Override
	protected Map<String, Integer> mapColumns(D data) {
		return EmptyDataRecordSet.COLUMNS;
	}

	@Override
	protected Iterator<R> getIterator(D data) {
		return Collections.emptyIterator();
	}

	@Override
	protected DataRecord newRecord(R record) {
		throw new IllegalStateException("Can't produce a record");
	}

	@Override
	protected void closeData(D data) {
	}
}