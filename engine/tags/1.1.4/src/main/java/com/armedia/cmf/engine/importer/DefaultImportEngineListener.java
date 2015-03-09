package com.armedia.cmf.engine.importer;

import java.util.Map;

import com.armedia.cmf.storage.StoredObjectType;

public class DefaultImportEngineListener extends DefaultImportListener implements ImportEngineListener {

	@Override
	public void importStarted(Map<StoredObjectType, Integer> summary) {
	}

	@Override
	public void objectTypeImportStarted(StoredObjectType objectType, int totalObjects) {
	}

	@Override
	public void objectTypeImportFinished(StoredObjectType objectType, Map<ImportResult, Integer> counters) {
	}

	@Override
	public void importFinished(Map<ImportResult, Integer> counters) {
	}
}