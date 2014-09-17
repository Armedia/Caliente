package com.delta.cmsmf.engine;

import com.delta.cmsmf.cms.CmsObjectType;

public interface CmsExportEventListener {

	/**
	 * <p>
	 * Invoked when the export has begun.
	 * </p>
	 *
	 */
	public void exportStarted();

	/**
	 * <p>
	 * Invoked when exporting objects of the given type has begun.
	 * </p>
	 *
	 * @param objectType
	 */
	public void objectTypeExportStarted(CmsObjectType objectType);

	/**
	 * <p>
	 * Invoked when export has started for the given object.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	public void objectExportStarted(CmsObjectType objectType, String objectId);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	public void objectExported(CmsObjectType objectType, String objectId);

	/**
	 * <p>
	 * Invoked when all the objects of the given type have been exported.
	 * </p>
	 *
	 * @param objectType
	 */
	public void objectTypeExportCompleted(CmsObjectType objectType);

	/**
	 * <p>
	 * Invoked when the export process has concluded.
	 * </p>
	 *
	 */
	public void exportConcluded();
}