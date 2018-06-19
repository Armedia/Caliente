package com.armedia.caliente.engine.exporter;

import java.util.UUID;

import com.armedia.caliente.engine.TransferListener;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;

public interface ExportListener extends TransferListener {

	/**
	 * <p>
	 * Invoked when export has started for the given object.
	 * </p>
	 */
	public void objectExportStarted(UUID jobId, CmfObjectRef object, CmfObjectRef referrent);

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
	public void objectSkipped(UUID jobId, CmfObjectRef object, ExportSkipReason reason, String extraInfo);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 */
	public void objectExportFailed(UUID jobId, CmfObjectRef object, Throwable thrown);

	/**
	 * <p>
	 * Invoked when a data consistency issue has been encountered, so it can be reported and tracked
	 * </p>
	 */
	public void consistencyWarning(UUID jobId, CmfObjectRef object, String fmt, Object... args);
}