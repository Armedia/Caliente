package com.armedia.caliente.cli.caliente.launcher;

import java.util.Set;
import java.util.UUID;

import com.armedia.caliente.engine.importer.DefaultImportEngineListener;
import com.armedia.caliente.store.CmfObject;

public class ImportRetryManifest extends DefaultImportEngineListener {

	private final RetryManifest manifest;

	public ImportRetryManifest(Set<CmfObject.Archetype> types) {
		this.manifest = new RetryManifest(types);
	}

	@Override
	public void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown) {
		this.manifest.logRetry(jobId, object, thrown);
	}
}