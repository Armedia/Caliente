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

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class SingleContentFinder extends ContentFinder {

	public SingleContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source, Consumer<IDfId> consumer) {
		super(pool, scannedIds, source, consumer);
	}

	@Override
	protected Stream<IDfId> getIds(IDfSession session) throws DfException {
		if (StringUtils.isBlank(this.source)) { return null; }
		if (!this.source.startsWith("%")) { return null; }
		IDfId id = new DfId(this.source.substring(1));
		if (id.isNull()) { return null; }
		if (!id.isObjectId()) { return null; }
		return Stream.of(id);
	}

}