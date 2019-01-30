package com.armedia.caliente.engine.importer;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.armedia.caliente.store.CmfArchetype;

public class DefaultImportEngineListener extends DefaultImportListener implements ImportEngineListener {

	private final Map<UUID, ImportState> jobStates = new ConcurrentHashMap<>();

	protected final ImportState getState(UUID uuid) {
		if (!this.jobStates.containsKey(uuid)) { throw new NoSuchElementException(
			String.format("Given job ID [%s] has no state stored", uuid.toString())); }
		return this.jobStates.get(uuid);
	}

	@Override
	public final void importStarted(ImportState importState, Map<CmfArchetype, Long> summary) {
		if (importState == null) { throw new IllegalArgumentException("Must provide a job import state"); }
		this.jobStates.put(importState.jobId, importState);
		importStartedImpl(importState, summary);

	}

	protected void importStartedImpl(ImportState importState, Map<CmfArchetype, Long> summary) {
	}

	@Override
	public void objectTypeImportStarted(UUID jobId, CmfArchetype objectType, long totalObjects) {
	}

	@Override
	public void objectTypeImportFinished(UUID jobId, CmfArchetype objectType, Map<ImportResult, Long> counters) {
	}

	@Override
	public final void importFinished(UUID jobId, Map<ImportResult, Long> counters) {
		try {
			importFinishedImpl(jobId, counters);
		} finally {
			this.jobStates.remove(jobId);
		}
	}

	protected void importFinishedImpl(UUID jobId, Map<ImportResult, Long> counters) {
	}
}