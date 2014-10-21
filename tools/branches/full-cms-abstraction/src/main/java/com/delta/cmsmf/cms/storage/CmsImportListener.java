package com.delta.cmsmf.cms.storage;

import com.delta.cmsmf.cms.CmsImportResult;

public interface CmsImportListener {
	/**
	 * <p>
	 * Invoked when the import has started for the given object.
	 * </p>
	 *
	 * @param object
	 */
	public void objectImportStarted(CmsObject object);

	/**
	 * <p>
	 * Invoked when the given object has been imported, and indicating the result of the import
	 * operation.
	 * </p>
	 *
	 * @param object
	 * @param cmsImportResult
	 */
	public void objectImportCompleted(CmsObject object, CmsImportResult cmsImportResult, String newLabel, String newId);

	/**
	 * <p>
	 * Invoked when the import attempt on the given object has failed, and indicating the exception
	 * that was raised.
	 * </p>
	 *
	 * @param object
	 * @param thrown
	 */
	public void objectImportFailed(CmsObject object, Throwable thrown);
}