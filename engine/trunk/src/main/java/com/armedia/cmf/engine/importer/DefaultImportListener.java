package com.armedia.cmf.engine.importer;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;

public class DefaultImportListener implements ImportListener {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void objectBatchImportStarted(CmfType objectType, String batchId, int count) {
	}

	@Override
	public void objectImportStarted(CmfObject<?> object) {
	}

	@Override
	public void objectImportCompleted(CmfObject<?> object, ImportOutcome outcome) {
	}

	@Override
	public void objectImportFailed(CmfObject<?> object, Throwable thrown) {
	}

	@Override
	public void objectBatchImportFinished(CmfType objectType, String batchId,
		Map<String, ImportOutcome> outcomes, boolean failed) {
	}

}