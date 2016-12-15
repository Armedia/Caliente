package com.armedia.caliente.engine.exporter;

import java.util.UUID;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;

public interface ExportListener {

	/**
	 * <p>
	 * Invoked when export has started for the given object.
	 * </p>
	 */
	public void objectExportStarted(UUID jobId, CmfType objectType, String objectId);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 */
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber);

	/**
	 * <p>
	 * Invoked when the given object has been skipped.
	 * </p>
	 */
	public void objectSkipped(UUID jobId, CmfType objectType, String objectId, ExportSkipReason reason,
		String extraInfo);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 */
	public void objectExportFailed(UUID jobId, CmfType objectType, String objectId, Throwable thrown);

	/**
	 * <p>
	 * Invoked when a data consistency issue has been encountered, so it can be reported and tracked
	 * </p>
	 */
	public void consistencyWarning(UUID jobId, CmfType objectType, String objectId, String fmt, Object... args);
}