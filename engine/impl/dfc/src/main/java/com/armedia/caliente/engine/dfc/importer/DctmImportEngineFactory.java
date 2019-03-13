package com.armedia.caliente.engine.dfc.importer;

import java.io.File;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.common.DctmCommon;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public class DctmImportEngineFactory extends
	ImportEngineFactory<IDfSession, IDfValue, DctmImportContext, DctmImportContextFactory, DctmImportDelegateFactory, DctmImportEngine> {

	public DctmImportEngineFactory() {
		super(true, new DctmCrypto());
	}

	@Override
	public DctmImportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings)
		throws ImportException {
		return new DctmImportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected Set<String> getTargetNames() {
		return DctmCommon.TARGETS;
	}
}