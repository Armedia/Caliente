package com.armedia.caliente.engine.importer;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;

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
	public void objectBatchImportStarted(UUID jobId, CmfType objectType, String batchId, int count);

	/**
	 * <p>
	 * Invoked when the import has started for the given object.
	 * </p>
	 *
	 * @param object
	 */
	public void objectImportStarted(UUID jobId, CmfObject<?> object);

	/**
	 * <p>
	 * Invoked when the given object has been imported, and indicating the outcome of the import
	 * operation.
	 * </p>
	 *
	 * @param object
	 * @param outcome
	 */
	public void objectImportCompleted(UUID jobId, CmfObject<?> object, ImportOutcome outcome);

	/**
	 * <p>
	 * Invoked when the import attempt on the given object has failed, and indicating the exception
	 * that was raised.
	 * </p>
	 *
	 * @param object
	 * @param thrown
	 */
	public void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown);

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
	public void objectBatchImportFinished(UUID jobId, CmfType objectType, String batchId,
		Map<String, Collection<ImportOutcome>> outcomes, boolean failed);
}