package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.alfresco.bi.AlfCommon;
import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;

public class AlfImportEngineFactory extends
	ImportEngineFactory<AlfRoot, CmfValue, AlfImportContext, AlfImportContextFactory, AlfImportDelegateFactory, AlfImportEngine> {

	public AlfImportEngineFactory() {
		super(false, new CmfCrypt());
	}

	@Override
	protected Set<String> getTargetNames() {
		return AlfCommon.TARGETS;
	}

	@Override
	public AlfImportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings)
		throws ImportException {
		return new AlfImportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

}