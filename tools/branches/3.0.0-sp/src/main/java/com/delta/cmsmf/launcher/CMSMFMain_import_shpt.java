package com.delta.cmsmf.launcher;

import java.util.Map;

import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportEngineListener;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.delta.cmsmf.exception.CMSMFException;

public class CMSMFMain_import_shpt extends AbstractCMSMFMain<ImportEngineListener, ImportEngine<?, ?, ?, ?, ?>>
implements ImportEngineListener {

	public CMSMFMain_import_shpt() throws Throwable {
		super(ImportEngine.getImportEngine("dctm"));
	}

	@Override
	public void run() throws CMSMFException {
		throw new CMSMFException("Sharepoint import is not currently supported");
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
	public void importStarted(Map<StoredObjectType, Integer> summary) {
	}

	@Override
	public void objectTypeImportStarted(StoredObjectType objectType, int totalObjects) {
	}

	@Override
	public void objectTypeImportFinished(StoredObjectType objectType, Map<ImportResult, Integer> counters) {
	}

	@Override
	public void importFinished(Map<ImportResult, Integer> counters) {
	}
}