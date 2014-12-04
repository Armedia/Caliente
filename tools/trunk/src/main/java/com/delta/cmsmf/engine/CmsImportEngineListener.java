package com.delta.cmsmf.engine;

import java.util.Map;

import com.delta.cmsmf.cms.CmsImportResult;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;

public interface CmsImportEngineListener {

	/**
	 * <p>
	 * Invoked when the import has begun and the total number of objects, of all types, that are
	 * expected to be processed. Please note that due to some import circumstances the actual number
	 * of objects operated upon may not match the total.
	 * </p>
	 *
	 * @param summary
	 */
	public void importStarted(Map<CmsObjectType, Integer> summary);

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
	 * Invoked when the import has begun on the given batch, and indicates how many objects the
	 * batch contains.
	 * </p>
	 *
	 * @param objectType
	 * @param batchId
	 * @param count
	 */
	public void objectBatchImportStarted(CmsObjectType objectType, String batchId, int count);

	/**
	 * <p>
	 * Invoked when the import has started for the given object.
	 * </p>
	 *
	 * @param object
	 */
	public void objectImportStarted(CmsObject<?> object);

	/**
	 * <p>
	 * Invoked when the given object has been imported, and indicating the result of the import
	 * operation.
	 * </p>
	 *
	 * @param object
	 * @param cmsImportResult
	 */
	public void objectImportCompleted(CmsObject<?> object, CmsImportResult cmsImportResult, String newLabel,
		String newId);

	/**
	 * <p>
	 * Invoked when the import attempt on the given object has failed, and indicating the exception
	 * that was raised.
	 * </p>
	 *
	 * @param object
	 * @param thrown
	 */
	public void objectImportFailed(CmsObject<?> object, Throwable thrown);

	/**
	 * <p>
	 * Invoked when the import has concluded on the given batch, and indicates how many objects were
	 * imported successfully, and whether the batch was failed early.
	 * </p>
	 *
	 * @param objectType
	 * @param batchId
	 * @param successful
	 * @param failed
	 */
	public void objectBatchImportCompleted(CmsObjectType objectType, String batchId, int successful, boolean failed);

	/**
	 * <p>
	 * Invoked when all the objects of the given type have been imported, indicating a breakdown of
	 * the actual counts of objects and their results.
	 * </p>
	 *
	 * @param objectType
	 * @param counters
	 */
	public void objectTypeImportFinished(CmsObjectType objectType, Map<CmsImportResult, Integer> counters);

	/**
	 * <p>
	 * Invoked when the import process has concluded, and with a breakdown including the total
	 * counters for all object types.
	 * </p>
	 *
	 * @param counters
	 */
	public void importFinished(Map<CmsImportResult, Integer> counters);
}