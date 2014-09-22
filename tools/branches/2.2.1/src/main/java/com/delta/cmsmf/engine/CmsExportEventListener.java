package com.delta.cmsmf.engine;

import java.util.Map;

import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;

public interface CmsExportEventListener {

	/**
	 * <p>
	 * Invoked when the export has begun.
	 * </p>
	 *
	 */
	public void exportStarted(String dql);

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
	 */
	public void objectExportCompleted(CmsObject<?> object);

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

	/**
	 * <p>
	 * Invoked when the export process has concluded.
	 * </p>
	 *
	 */
	public void exportFinished(Map<CmsObjectType, Integer> summary);
}