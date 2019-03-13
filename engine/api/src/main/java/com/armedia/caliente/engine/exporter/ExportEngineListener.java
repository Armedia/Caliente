package com.armedia.caliente.engine.exporter;

import java.util.Map;
import java.util.UUID;

import com.armedia.caliente.store.CmfObject;

public interface ExportEngineListener extends ExportListener {

	/**
	 * <p>
	 * Invoked when the export has begun.
	 * </p>
	 */
	public void exportStarted(ExportState exportState);

	/**
	 * <p>
	 * Invoked when the search for export target begins for a given source
	 * </p>
	 */
	public void sourceSearchStarted(String source);

	/**
	 * <p>
	 * Invoked periodically when a certain number of items is retrieved from the given source
	 * </p>
	 */
	public void sourceSearchMilestone(String source, long sourceCount, long totalCount);

	/**
	 * <p>
	 * Invoked when all the items from the given source have been retrieved
	 * </p>
	 */
	public void sourceSearchCompleted(String source, long sourceCount, long totalCount);

	/**
	 * <p>
	 * Invoked when the search for items within a given source has failed
	 * </p>
	 */
	public void sourceSearchFailed(String source, long sourceCount, long totalCount, Exception thrown);

	/**
	 * <p>
	 * Invoked when all the items from all given sources have been retrieved
	 * </p>
	 */
	public void searchCompleted(long totalCount);

	/**
	 * <p>
	 * Invoked when searching has failed and we can no longer continue
	 * </p>
	 */
	public void searchFailed(long totalCount, Exception thrown);

	/**
	 * <p>
	 * Invoked when the export process has concluded.
	 * </p>
	 */
	public void exportFinished(UUID jobId, Map<CmfObject.Archetype, Long> summary);
}