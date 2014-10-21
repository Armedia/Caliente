package com.delta.cmsmf.cms.storage;

public interface CmsExportListener {

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
	 * @param object
	 * @param num
	 */
	public void objectExportCompleted(CmsObject object, Long num);

	/**
	 * <p>
	 * Invoked when the given object has been skipped.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	public void objectSkipped(CmsObjectType objectType, String objectId);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param objectType
	 * @param thrown
	 */
	public void objectExportFailed(CmsObjectType objectType, String objectId, Throwable thrown);

}