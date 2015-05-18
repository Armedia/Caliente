package com.armedia.cmf.engine.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;

public class DefaultExportListener implements ExportListener {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void objectExportStarted(CmfType objectType, String objectId) {
	}

	@Override
	public void objectExportCompleted(CmfObject<?> object, Long objectNumber) {
	}

	@Override
	public void objectSkipped(CmfType objectType, String objectId) {
	}

	@Override
	public void objectExportFailed(CmfType objectType, String objectId, Throwable thrown) {
	}
}