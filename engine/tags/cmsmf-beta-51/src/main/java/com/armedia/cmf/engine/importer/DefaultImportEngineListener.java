package com.armedia.cmf.engine.importer;

import java.util.Map;
import java.util.UUID;

import com.armedia.cmf.storage.CmfType;

public class DefaultImportEngineListener extends DefaultImportListener implements ImportEngineListener {

	@Override
	public void importStarted(UUID jobId, Map<CmfType, Integer> summary) {
	}

	@Override
	public void objectTypeImportStarted(UUID jobId, CmfType objectType, int totalObjects) {
	}

	@Override
	public void objectTypeImportFinished(UUID jobId, CmfType objectType, Map<ImportResult, Integer> counters) {
	}

	@Override
	public void importFinished(UUID jobId, Map<ImportResult, Integer> counters) {
	}
}