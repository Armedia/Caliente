package com.armedia.cmf.engine.importer;

import java.util.Map;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;

public interface ImportListener {

	/**
	 * <p>
	 * Invoked when the import of a batch of objects has begun, and the estimated number of objects
	 * that will be processed.
	 * </p>
	 *
	 * @param objectType
	 * @param batchId
	 * @param count
	 */
	public void objectBatchImportStarted(CmfType objectType, String batchId, int count);

	/**
	 * <p>
	 * Invoked when the import has started for the given object.
	 * </p>
	 *
	 * @param object
	 */
	public void objectImportStarted(CmfObject<?> object);

	/**
	 * <p>
	 * Invoked when the given object has been imported, and indicating the outcome of the import
	 * operation.
	 * </p>
	 *
	 * @param object
	 * @param outcome
	 */
	public void objectImportCompleted(CmfObject<?> object, ImportOutcome outcome);

	/**
	 * <p>
	 * Invoked when the import attempt on the given object has failed, and indicating the exception
	 * that was raised.
	 * </p>
	 *
	 * @param object
	 * @param thrown
	 */
	public void objectImportFailed(CmfObject<?> object, Throwable thrown);

	/**
	 * <p>
	 * Invoked when the import of a batch has finished.
	 * </p>
	 *
	 * @param objectType
	 * @param batchId
	 * @param outcomes
	 * @param failed
	 */
	public void objectBatchImportFinished(CmfType objectType, String batchId,
		Map<String, ImportOutcome> outcomes, boolean failed);
}