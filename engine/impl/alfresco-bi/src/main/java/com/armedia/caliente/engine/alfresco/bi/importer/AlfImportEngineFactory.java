package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBException;

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
import com.armedia.commons.utilities.CfgTools;

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
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings)
		throws ImportException {
		try {
			return new AlfImportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
		} catch (IOException | JAXBException e) {
			throw new ImportException("Failed to build a new AlfImportEngine instance", e);
		}
	}

}