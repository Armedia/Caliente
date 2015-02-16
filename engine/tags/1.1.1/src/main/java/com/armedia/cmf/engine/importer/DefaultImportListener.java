package com.armedia.cmf.engine.importer;

import com.armedia.cmf.storage.StoredObject;

public class DefaultImportListener implements ImportListener {

	@Override
	public void objectImportStarted(StoredObject<?> object) {
	}

	@Override
	public void objectImportCompleted(StoredObject<?> object, ImportOutcome outcome) {
	}

	@Override
	public void objectImportFailed(StoredObject<?> object, Throwable thrown) {
	}

}