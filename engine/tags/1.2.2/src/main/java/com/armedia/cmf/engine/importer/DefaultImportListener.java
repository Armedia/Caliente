package com.armedia.cmf.engine.importer;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

public class DefaultImportListener implements ImportListener {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void objectBatchImportStarted(StoredObjectType objectType, String batchId, int count) {
	}

	@Override
	public void objectImportStarted(StoredObject<?> object) {
	}

	@Override
	public void objectImportCompleted(StoredObject<?> object, ImportOutcome outcome) {
	}

	@Override
	public void objectImportFailed(StoredObject<?> object, Throwable thrown) {
	}

	@Override
	public void objectBatchImportFinished(StoredObjectType objectType, String batchId,
		Map<String, ImportOutcome> outcomes, boolean failed) {
	}

}