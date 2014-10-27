package com.delta.cmsmf.engine;

import java.util.Map;

import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

public class DefaultImportEventListener implements ImportEngineListener {

	@Override
	public void importStarted(Map<StoredObjectType, Integer> summary) {
	}

	@Override
	public void objectTypeImportStarted(StoredObjectType objectType, int totalObjects) {
	}

	@Override
	public void objectImportStarted(StoredObject<?> object) {
	}

	@Override
	public void objectImportCompleted(StoredObject<?> object, ImportResult cmsImportResult, String newLabel,
		String newId) {
	}

	@Override
	public void objectImportFailed(StoredObject<?> object, Throwable thrown) {
	}

	@Override
	public void objectTypeImportFinished(StoredObjectType objectType, Map<ImportResult, Integer> counters) {
	}

	@Override
	public void importFinished(Map<ImportResult, Integer> counters) {
	}
}