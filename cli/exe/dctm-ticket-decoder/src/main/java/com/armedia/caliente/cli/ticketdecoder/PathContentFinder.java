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

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class PathContentFinder extends PredicateContentFinder {

	public PathContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source, Consumer<IDfId> consumer) {
		super(pool, scannedIds, source, consumer);
	}

	@Override
	protected Stream<IDfId> getIds(IDfSession session) throws DfException {
		// Source is a path to either a folder or a document
		IDfPersistentObject obj = session.getObjectByPath(this.source);
		if (obj == null) { return null; }

		final IDfId id = obj.getObjectId();
		if (!obj.isInstanceOf("dm_folder")) {
			// Not a folder, so no recursion!
			return Stream.of(id);
		}

		// If it's a folder, we morph into a query-based recursion.
		return super.getIds(session,
			String.format("dm_sysobject where folder(id(%s), DESCEND)", DfcUtils.quoteString(id.getId())));
	}
}