package com.armedia.caliente.engine.xml.importer;

import java.io.File;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.common.XmlCommon;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class XmlImportEngineFactory extends
	ImportEngineFactory<XmlRoot, CmfValue, XmlImportContext, XmlImportContextFactory, XmlImportDelegateFactory, XmlImportEngine> {

	public XmlImportEngineFactory() {
		super(false, new CmfCrypt());
	}

	@Override
	public XmlImportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings)
		throws ImportException {
		return new XmlImportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected Set<String> getTargetNames() {
		return XmlCommon.TARGETS;
	}
}