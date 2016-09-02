package com.armedia.cmf.engine.exporter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;

public class DefaultExportListener implements ExportListener {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void objectExportStarted(UUID jobId, CmfType objectType, String objectId) {
	}

	@Override
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
	}

	@Override
	public void objectSkipped(UUID jobId, CmfType objectType, String objectId, ExportSkipReason reason) {
	}

	@Override
	public void objectExportFailed(UUID jobId, CmfType objectType, String objectId, Throwable thrown) {
	}
}