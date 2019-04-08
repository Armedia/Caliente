package com.armedia.caliente.engine.dfc.exporter;

import java.io.File;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.common.DctmCommon;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.dfc.DfcCrypto;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public class DctmExportEngineFactory extends
	ExportEngineFactory<IDfSession, IDfValue, DctmExportContext, DctmExportContextFactory, DctmExportDelegateFactory, DctmExportEngine> {

	public DctmExportEngineFactory() {
		super(true, new DfcCrypto());
	}

	@Override
	public DctmExportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings)
		throws ExportException {
		return new DctmExportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected Set<String> getTargetNames() {
		return DctmCommon.TARGETS;
	}

}