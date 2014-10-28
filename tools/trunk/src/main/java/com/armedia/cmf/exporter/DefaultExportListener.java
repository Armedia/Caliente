package com.armedia.cmf.exporter;

import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

public class DefaultExportListener implements ExportListener {

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