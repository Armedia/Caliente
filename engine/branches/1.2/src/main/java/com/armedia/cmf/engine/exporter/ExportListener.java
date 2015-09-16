package com.armedia.cmf.engine.exporter;

import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

public interface ExportListener {

	/**
	 * <p>
	 * Invoked when export has started for the given object.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	public void objectExportStarted(StoredObjectType objectType, String objectId);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param object
	 */
	public void objectExportCompleted(StoredObject<?> object, Long objectNumber);

	/**
	 * <p>
	 * Invoked when the given object has been skipped.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	public void objectSkipped(StoredObjectType objectType, String objectId, String reason);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param objectType
	 * @param thrown
	 */
	public void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown);

}