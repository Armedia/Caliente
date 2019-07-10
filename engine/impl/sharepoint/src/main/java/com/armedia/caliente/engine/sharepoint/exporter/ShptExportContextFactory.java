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
/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

/**
 *
 *
 */
public class ShptExportContextFactory
	extends ExportContextFactory<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportEngine> {

	ShptExportContextFactory(ShptExportEngine engine, CfgTools settings, ShptSession session,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Logger output, WarningTracker warningTracker)
		throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
	}

	@Override
	protected ShptExportContext constructContext(String rootId, CmfObject.Archetype rootType, ShptSession session,
		int batchPosition) {
		return new ShptExportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker());
	}

	@Override
	protected String calculateProductName(ShptSession session) throws Exception {
		return "Sharepoint";
	}

	@Override
	protected String calculateProductVersion(ShptSession session) throws Exception {
		return session.getContextInfo().getLibraryVersion();
	}
}