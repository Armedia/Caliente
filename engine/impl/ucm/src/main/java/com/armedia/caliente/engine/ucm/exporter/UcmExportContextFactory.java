package com.armedia.caliente.engine.ucm.exporter;

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
import com.armedia.caliente.engine.ucm.model.UcmRuntimeException;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceResponse;

public class UcmExportContextFactory
	extends ExportContextFactory<UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportEngine> {

	private UcmAttributes systemInfo = null;
	private Map<String, DataResultSet> serverData = null;

	private static synchronized void initializeConnectionData(UcmExportContextFactory factory, UcmSession session)
		throws UcmServiceException, IdcClientException {
		if ((factory.systemInfo == null) && (factory.serverData == null)) {
			ServiceResponse rsp = session.callService("CONFIG_INFO");
			DataBinder binder = rsp.getResponseAsBinder();

			Map<String, DataResultSet> m = new TreeMap<>();
			for (String rsName : binder.getResultSetNames()) {
				m.put(rsName, binder.getResultSet(rsName));
			}
			factory.systemInfo = new UcmAttributes(binder.getLocalData(), binder);
			factory.serverData = Tools.freezeMap(new LinkedHashMap<>(m));
		}
	}

	UcmExportContextFactory(UcmExportEngine engine, UcmSession session, CfgTools settings,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Logger output,
		WarningTracker warningTracker) throws Exception {
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
	protected UcmExportContext constructContext(String rootId, CmfType rootType, UcmSession session,
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
		return this.systemInfo.getString(UcmAtt.ProductBuildInfo);
	}
}