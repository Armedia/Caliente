package com.armedia.cmf.engine.exporter;

import java.util.Map;

import com.armedia.cmf.storage.CmfType;

public class DefaultExportEngineListener extends DefaultExportListener implements ExportEngineListener {

	@Override
	public void exportStarted(ExportState exportState) {
	}

	@Override
	public void exportFinished(ExportState exportState, Map<CmfType, Integer> summary) {
	}
}