package com.armedia.cmf.engine.importer;

import com.armedia.cmf.storage.StoredObject;

public class DefaultImportListener implements ImportListener {

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

}