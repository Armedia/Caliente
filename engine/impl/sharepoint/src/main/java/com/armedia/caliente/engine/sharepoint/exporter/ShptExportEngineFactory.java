package com.armedia.caliente.engine.sharepoint.exporter;

import java.io.File;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.sharepoint.ShptCommon;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

public class ShptExportEngineFactory extends
	ExportEngineFactory<ShptSession, CmfValue, ShptExportContext, ShptExportContextFactory, ShptExportDelegateFactory, ShptExportEngine> {

	public ShptExportEngineFactory() {
		super(false, new CmfCrypt());
	}

	@Override
	public ShptExportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings)
		throws ExportException {
		return new ShptExportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected Set<String> getTargetNames() {
		return ShptCommon.TARGETS;
	}

}