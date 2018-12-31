package com.armedia.caliente.engine.xds.importer;

import java.io.File;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xds.CmisCommon;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportEngineFactory extends
	ImportEngineFactory<Session, CmfValue, CmisImportContext, CmisImportContextFactory, CmisImportDelegateFactory, CmisImportEngine> {

	public CmisImportEngineFactory() {
		super(false, new CmfCrypt());
		// TODO Auto-generated constructor stub
	}

	@Override
	public CmisImportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings)
		throws ImportException {
		return new CmisImportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected Set<String> getTargetNames() {
		return CmisCommon.TARGETS;
	}
}