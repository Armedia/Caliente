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

package com.armedia.caliente.engine.dfc.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.common.DctmSpecialValues;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmExportContextFactory
	extends ExportContextFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportEngine> {

	private final DctmSpecialValues specialValues;

	DctmExportContextFactory(DctmExportEngine engine, CfgTools settings, IDfSession session,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Logger output, WarningTracker warningTracker)
		throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
		this.specialValues = new DctmSpecialValues(settings);
	}

	@Override
	protected DctmExportContext constructContext(String rootId, CmfObject.Archetype rootType, IDfSession session,
		int batchPosition) {
		return new DctmExportContext(this, rootId, rootType, session, getOutput(), getWarningTracker());
	}

	public final DctmSpecialValues getSpecialValues() {
		return this.specialValues;
	}

	@Override
	protected final String calculateProductName(IDfSession session) {
		return "Documentum";
	}

	@Override
	protected final String calculateProductVersion(IDfSession session) throws Exception {
		return session.getServerVersion();
	}
}