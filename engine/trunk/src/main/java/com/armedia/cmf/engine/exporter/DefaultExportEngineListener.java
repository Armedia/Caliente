package com.armedia.cmf.engine.exporter;

import java.util.Map;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;

public class DefaultExportEngineListener extends DefaultExportListener implements ExportEngineListener {

	@Override
	public void exportStarted(CfgTools configuration) {
	}

	@Override
	public void exportFinished(Map<StoredObjectType, Integer> summary) {
	}
}