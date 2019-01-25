package com.armedia.caliente.engine.exporter;

import java.util.UUID;

import com.armedia.caliente.engine.TransferListener;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectSearchSpec;

public interface ExportListener extends TransferListener {

	/**
	 * <p>
	 * Invoked when export has started for the given object.
	 * </p>
	 */
	public void objectExportStarted(UUID jobId, CmfObjectSearchSpec object, CmfObjectSearchSpec referrent);

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
	public void objectSkipped(UUID jobId, CmfObjectSearchSpec object, ExportSkipReason reason, String extraInfo);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 */
	public void objectExportFailed(UUID jobId, CmfObjectSearchSpec object, Throwable thrown);

	/**
	 * <p>
	 * Invoked when a data consistency issue has been encountered, so it can be reported and tracked
	 * </p>
	 */
	public void consistencyWarning(UUID jobId, CmfObjectSearchSpec object, String fmt, Object... args);
}