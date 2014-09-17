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
	 * @param objectId
	 */
	public void objectExportFinished(CmsObjectType objectType, String objectId, CmsExportResult result, Throwable thrown);

	/**
	 * <p>
	 * Invoked when the export process has concluded.
	 * </p>
	 *
	 */
	public void exportConcluded(Map<CmsObjectType, Integer> summary);
}