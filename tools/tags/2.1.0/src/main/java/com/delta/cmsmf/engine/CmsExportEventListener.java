package com.delta.cmsmf.engine;

import java.util.Map;

import com.delta.cmsmf.cms.CmsExportResult;
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
	 * @param objectType
	 * @param result
	 */
	public void objectExportCompleted(CmsObjectType objectType, String objectId, CmsExportResult result);

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