package com.armedia.caliente.engine.importer;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;

public class DefaultImportListener implements ImportListener {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void objectBatchImportStarted(UUID jobId, CmfType objectType, String batchId, int count) {
	}

	@Override
	public void objectImportStarted(UUID jobId, CmfObject<?> object) {
	}

	@Override
	public void objectImportCompleted(UUID jobId, CmfObject<?> object, ImportOutcome outcome) {
	}

	@Override
	public void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown) {
	}

	@Override
	public void objectBatchImportFinished(UUID jobId, CmfType objectType, String batchId,
		Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
	}

}