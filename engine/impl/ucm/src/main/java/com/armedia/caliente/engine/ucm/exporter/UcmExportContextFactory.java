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
package com.armedia.caliente.engine.ucm.exporter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.engine.ucm.model.UcmAtt;
import com.armedia.caliente.engine.ucm.model.UcmAttributes;
import com.armedia.caliente.engine.ucm.model.UcmModel;
import com.armedia.caliente.engine.ucm.model.UcmRuntimeException;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataResultSet;

public class UcmExportContextFactory
	extends ExportContextFactory<UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportEngine> {

	private String serverVersion = null;
	private UcmAttributes systemInfo = null;
	private Map<String, DataResultSet> serverData = null;

	private static void initializeConnectionData(UcmExportContextFactory factory, UcmSession session)
		throws UcmServiceException, IdcClientException {
		if ((factory.systemInfo == null) && (factory.serverData == null)) {
			DataBinder binder = UcmModel.getConfigInfo(session);
			if (binder == null) {
				factory.serverData = Collections.emptyMap();
				factory.serverVersion = "0.0.0";
				factory.log.warn(
					"User [{}] does not have permissions to read system configuration information - will proceed without it but this may cause issues later on",
					session.getUserContext().getUser());
				return;
			}

			Map<String, DataResultSet> m = new TreeMap<>();
			for (String rsName : binder.getResultSetNames()) {
				m.put(rsName, binder.getResultSet(rsName));
			}
			factory.systemInfo = new UcmAttributes(binder.getLocalData(), binder);
			factory.serverData = Tools.freezeMap(new LinkedHashMap<>(m));
			factory.serverVersion = factory.systemInfo.getString(UcmAtt.ProductBuildInfo);
		}
	}

	UcmExportContextFactory(UcmExportEngine engine, UcmSession session, CfgTools settings,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Logger output, WarningTracker warningTracker)
		throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
		UcmExportContextFactory.initializeConnectionData(this, session);
	}

	public final UcmAttributes getSystemInfo() {
		return this.systemInfo;
	}

	public final Map<String, DataResultSet> getServerData() {
		return this.serverData;
	}

	@Override
	protected UcmExportContext constructContext(String rootId, CmfObject.Archetype rootType, UcmSession session,
		int batchPosition) {
		return new UcmExportContext(this, rootId, rootType, session, getOutput(), getWarningTracker());
	}

	@Override
	public final String calculateProductName(UcmSession session) {
		return "Oracle WebCenter";
	}

	@Override
	public final String calculateProductVersion(UcmSession session) {
		try {
			UcmExportContextFactory.initializeConnectionData(this, session);
		} catch (UcmServiceException | IdcClientException e) {
			throw new UcmRuntimeException("Failed to query the server version data", e);
		}
		return this.serverVersion;
	}
}