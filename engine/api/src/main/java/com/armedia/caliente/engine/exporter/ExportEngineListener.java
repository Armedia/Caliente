package com.armedia.caliente.engine.exporter;

import java.util.Map;
import java.util.UUID;

import com.armedia.caliente.store.CmfArchetype;

public interface ExportEngineListener extends ExportListener {

	/**
	 * <p>
	 * Invoked when the export has begun.
	 * </p>
	 *
	 */
	public void exportStarted(ExportState exportState);

	/**
	 * <p>
	 * Invoked when the export process has concluded.
	 * </p>
	 *
	 */
	public void exportFinished(UUID jobId, Map<CmfArchetype, Long> summary);
}