package com.delta.cmsmf.engine;

import java.util.Map;

import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

public class DefaultExportEventListener implements ExportEngineListener {

	@Override
	public void exportStarted(String dql) {
	}

	@Override
	public void objectExportStarted(StoredObjectType objectType, String objectId) {
	}

	@Override
	public void objectExportCompleted(StoredObject<?> object) {
	}

	@Override
	public void objectSkipped(StoredObjectType objectType, String objectId) {
	}

	@Override
	public void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown) {
	}

	@Override
	public void exportFinished(Map<StoredObjectType, Integer> summary) {
	}
}