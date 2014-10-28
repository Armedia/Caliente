package com.armedia.cmf.exporter;

import java.util.Map;

import com.armedia.cmf.storage.StoredObjectType;

public class DefaultExportEngineListener extends DefaultExportListener implements ExportEngineListener {

	@Override
	public void exportStarted(Map<String, Object> settings) {
	}

	@Override
	public void exportFinished(Map<StoredObjectType, Integer> summary) {
	}
}