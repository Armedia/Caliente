package com.armedia.cmf.engine.importer;

import java.util.Map;

import com.armedia.cmf.storage.CmfType;

public class DefaultImportEngineListener extends DefaultImportListener implements ImportEngineListener {

	@Override
	public void importStarted(Map<CmfType, Integer> summary) {
	}

	@Override
	public void objectTypeImportStarted(CmfType objectType, int totalObjects) {
	}

	@Override
	public void objectTypeImportFinished(CmfType objectType, Map<ImportResult, Integer> counters) {
	}

	@Override
	public void importFinished(Map<ImportResult, Integer> counters) {
	}
}