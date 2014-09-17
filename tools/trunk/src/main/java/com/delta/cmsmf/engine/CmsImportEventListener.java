package com.delta.cmsmf.engine;

import java.util.Map;

import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.CmsImportResult;

public interface CmsImportEventListener {

	/**
	 * <p>
	 * Invoked when the import has begun and the total number of objects, of all types, that are
	 * expected to be processed. Please note that due to some import circumstances the actual number
	 * of objects operated upon may not match the total.
	 * </p>
	 *
	 * @param totalObjects
	 */
	public void importStarted(int totalObjects);

	/**
	 * <p>
	 * Invoked when importing objects of the given type has begun, and the estimated number of
	 * objects that will be processed.
	 * </p>
	 *
	 * @param objectType
	 * @param totalObjects
	 */
	public void objectTypeImportStarted(CmsObjectType objectType, int totalObjects);

	/**
	 * <p>
	 * Invoked when the import has started for the given object.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	public void objectImportStarted(CmsObjectType objectType, String objectId);

	/**
	 * <p>
	 * Invoked when the given object has been imported, and indicating the result of the import
	 * operation.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 * @param cmsImportResult
	 */
	public void objectImported(CmsObjectType objectType, String objectId, CmsImportResult cmsImportResult);

	/**
	 * <p>
	 * Invoked when all the objects of the given type have been imported, indicating a breakdown of
	 * the actual counts of objects and their results.
	 * </p>
	 *
	 * @param objectType
	 * @param counters
	 */
	public void objectTypeImportCompleted(CmsObjectType objectType, Map<CmsImportResult, Integer> counters);

	/**
	 * <p>
	 * Invoked when the import process has concluded, and with a breakdown including the total
	 * counters for all object types.
	 * </p>
	 *
	 * @param counters
	 */
	public void importConcluded(Map<CmsImportResult, Integer> counters);
}