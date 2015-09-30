package com.armedia.cmf.engine.exporter;

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
	public void objectExportStarted(CmfType objectType, String objectId);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param object
	 */
	public void objectExportCompleted(CmfObject<?> object, Long objectNumber);

	/**
	 * <p>
	 * Invoked when the given object has been skipped.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 * @param reason
	 */
	public void objectSkipped(CmfType objectType, String objectId, ExportSkipReason reason);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param objectType
	 * @param thrown
	 */
	public void objectExportFailed(CmfType objectType, String objectId, Throwable thrown);

}