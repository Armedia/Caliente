package com.armedia.caliente.engine.exporter;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.armedia.caliente.store.CmfArchetype;

public class DefaultExportEngineListener extends DefaultExportListener implements ExportEngineListener {

	private final Map<UUID, ExportState> jobStates = new ConcurrentHashMap<>();

	protected final ExportState getState(UUID uuid) {
		if (uuid == null) { throw new NullPointerException("Must provide a job ID to find"); }
		if (!this.jobStates.containsKey(uuid)) { throw new NoSuchElementException(
			String.format("Given job ID [%s] has no state stored", uuid.toString())); }
		return this.jobStates.get(uuid);
	}

	@Override
	public final void exportStarted(ExportState exportState) {
		if (exportState == null) { throw new IllegalArgumentException("Must provide a job export state"); }
		this.jobStates.put(exportState.jobId, exportState);
		exportStartedImpl(exportState);
	}

	protected void exportStartedImpl(ExportState exportState) {
		// Do whatever...
	}

	@Override
	public void exportFinished(UUID jobId, Map<CmfArchetype, Long> summary) {
		try {
			exportFinishedImpl(jobId, summary);
		} finally {
			this.jobStates.remove(jobId);
		}
	}

	protected void exportFinishedImpl(UUID jobId, Map<CmfArchetype, Long> summary) {
		// Do whatever...
	}
}