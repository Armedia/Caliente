package com.armedia.cmf.exporter;

import java.util.Map;

import com.armedia.cmf.storage.StoredObjectType;

public interface ExportEngineListener extends ExportListener {

	/**
	 * <p>
	 * Invoked when the export has begun.
	 * </p>
	 *
	 */
	public void exportStarted(Map<String, Object> exportSettings);

	/**
	 * <p>
	 * Invoked when the export process has concluded.
	 * </p>
	 *
	 */
	public void exportFinished(Map<StoredObjectType, Integer> summary);
}