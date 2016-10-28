package com.armedia.cmf.engine.importer;

import java.util.Map;
import java.util.UUID;

import com.armedia.cmf.storage.CmfType;

public interface ImportEngineListener extends ImportListener {

	/**
	 * <p>
	 * Invoked when the import has begun and the total number of objects, of all types, that are
	 * expected to be processed. Please note that due to some import circumstances the actual number
	 * of objects operated upon may not match the total.
	 * </p>
	 *
	 * @param summary
	 */
	public void importStarted(ImportState importState, Map<CmfType, Long> summary);

	/**
	 * <p>
	 * Invoked when importing objects of the given type has begun, and the estimated number of
	 * objects that will be processed.
	 * </p>
	 *
	 * @param objectType
	 * @param totalObjects
	 */
	public void objectTypeImportStarted(UUID jobId, CmfType objectType, long totalObjects);

	/**
	 * <p>
	 * Invoked when all the objects of the given type have been imported, indicating a breakdown of
	 * the actual counts of objects and their results.
	 * </p>
	 *
	 * @param objectType
	 * @param counters
	 */
	public void objectTypeImportFinished(UUID jobId, CmfType objectType, Map<ImportResult, Long> counters);

	/**
	 * <p>
	 * Invoked when the import process has concluded, and with a breakdown including the total
	 * counters for all object types.
	 * </p>
	 *
	 * @param counters
	 */
	public void importFinished(UUID jobId, Map<ImportResult, Long> counters);
}