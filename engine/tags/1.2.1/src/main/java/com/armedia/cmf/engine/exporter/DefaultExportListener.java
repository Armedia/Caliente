package com.armedia.cmf.engine.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

public class DefaultExportListener implements ExportListener {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void objectExportStarted(StoredObjectType objectType, String objectId) {
	}

	@Override
	public void objectExportCompleted(StoredObject<?> object, Long objectNumber) {
	}

	@Override
	public void objectSkipped(StoredObjectType objectType, String objectId) {
	}

	@Override
	public void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown) {
	}
}