package com.armedia.caliente.engine.ucm.exporter;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.ucm.UcmCommon;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;

public class UcmExportEngineFactory extends
	ExportEngineFactory<UcmSession, CmfValue, UcmExportContext, UcmExportContextFactory, UcmExportDelegateFactory, UcmExportEngine> {

	public UcmExportEngineFactory() {
		super(false, new CmfCrypt());
	}

	@Override
	public UcmExportEngine newInstance(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings)
		throws ExportException {
		return new UcmExportEngine(this, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected Set<String> getTargetNames() {
		return UcmCommon.TARGETS;
	}
}