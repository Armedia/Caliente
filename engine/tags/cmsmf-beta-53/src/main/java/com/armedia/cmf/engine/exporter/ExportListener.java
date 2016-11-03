package com.armedia.cmf.engine.exporter;

import java.util.UUID;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;

public interface ExportListener {

	/**
	 * <p>
	 * Invoked when export has started for the given object.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	public void objectExportStarted(UUID jobId, CmfType objectType, String objectId);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param object
	 */
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber);

	/**
	 * <p>
	 * Invoked when the given object has been skipped.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 * @param reason
	 */
	public void objectSkipped(UUID jobId, CmfType objectType, String objectId, ExportSkipReason reason,
		String extraInfo);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param objectType
	 * @param thrown
	 */
	public void objectExportFailed(UUID jobId, CmfType objectType, String objectId, Throwable thrown);

}