package com.delta.cmsmf.engine;

import java.util.Map;

import com.delta.cmsmf.cms.CmsExportResult;
import com.delta.cmsmf.cms.CmsObjectType;

public class DefaultCmsExportEventListener implements CmsExportEventListener {

	/**
	 * <p>
	 * Invoked when the export has begun.
	 * </p>
	 *
	 */
	@Override
	public void exportStarted(String dql) {
	}

	/**
	 * <p>
	 * Invoked when exporting objects of the given type has begun.
	 * </p>
	 *
	 * @param objectType
	 */
	@Override
	public void objectTypeExportStarted(CmsObjectType objectType) {
	}

	/**
	 * <p>
	 * Invoked when export has started for the given object.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	@Override
	public void objectExportStarted(CmsObjectType objectType, String objectId) {
	}

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 *
	 * @param objectType
	 * @param objectId
	 */
	@Override
	public void objectExportFinished(CmsObjectType objectType, String objectId, CmsExportResult result, Throwable thrown) {
	}

	/**
	 * <p>
	 * Invoked when all the objects of the given type have been exported.
	 * </p>
	 *
	 * @param objectType
	 */
	@Override
	public void objectTypeExportCompleted(CmsObjectType objectType) {
	}

	/**
	 * <p>
	 * Invoked when the export process has concluded.
	 * </p>
	 *
	 */
	@Override
	public void exportConcluded(Map<CmsObjectType, Integer> summary) {
	}
}