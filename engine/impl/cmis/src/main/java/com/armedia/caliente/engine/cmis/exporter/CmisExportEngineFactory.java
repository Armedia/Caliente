package com.armedia.caliente.engine.cmis.exporter;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.cmis.CmisCommon;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;

public class CmisExportEngineFactory extends
	ExportEngineFactory<Session, CmfValue, CmisExportContext, CmisExportContextFactory, CmisExportDelegateFactory, CmisExportEngine> {

	public CmisExportEngineFactory() {
		super(false, new CmfCrypt());
	}

	@Override
	public CmisExportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings)
		throws ExportException {
		return new CmisExportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected Set<String> getTargetNames() {
		return CmisCommon.TARGETS;
	}

}