package com.armedia.caliente.engine.importer;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.armedia.caliente.engine.TransferListener;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfArchetype;

public interface ImportListener extends TransferListener {

	/**
	 * <p>
	 * Invoked when the import of a history of objects has begun, and the estimated number of
	 * objects that will be processed.
	 * </p>
	 *
	 * @param objectType
	 * @param tier
	 */
	public void objectTierImportStarted(UUID jobId, CmfArchetype objectType, int tier);

	/**
	 * <p>
	 * Invoked when the import of a history of objects has begun, and the estimated number of
	 * objects that will be processed.
	 * </p>
	 *
	 * @param objectType
	 * @param historyId
	 * @param count
	 */
	public void objectHistoryImportStarted(UUID jobId, CmfArchetype objectType, String historyId, int count);

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
	 * Invoked when the import of a history has finished.
	 * </p>
	 *
	 * @param objectType
	 * @param historyId
	 * @param outcomes
	 * @param failed
	 */
	public void objectHistoryImportFinished(UUID jobId, CmfArchetype objectType, String historyId,
		Map<String, Collection<ImportOutcome>> outcomes, boolean failed);

	/**
	 * <p>
	 * Invoked when the import of a dependency tier has finished.
	 * </p>
	 *
	 * @param jobId
	 * @param objectType
	 * @param tier
	 * @param failed
	 */
	public void objectTierImportFinished(UUID jobId, CmfArchetype objectType, int tier, boolean failed);
}