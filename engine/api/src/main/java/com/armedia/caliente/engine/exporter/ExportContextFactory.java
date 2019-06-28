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
package com.armedia.caliente.engine.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportContextFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ExportContext<SESSION, VALUE, ?>, //
	ENGINE extends ExportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?> //
> extends TransferContextFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	protected ExportContextFactory(ENGINE engine, CfgTools settings, SESSION session, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> contentStore, Logger output, WarningTracker tracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, null, output, tracker);
	}

	@Override
	protected String getContextLabel() {
		return "export";
	}
}