package com.delta.cmsmf.engine;

import java.util.Map;

import com.delta.cmsmf.cms.CmsExportListener;
import com.delta.cmsmf.cms.CmsObjectType;

public interface CmsExportEngineListener extends CmsExportListener {

	/**
	 * <p>
	 * Invoked when the export has begun.
	 * </p>
	 *
	 */
	public void exportStarted(String dql);

	/**
	 * <p>
	 * Invoked when the export process has concluded.
	 * </p>
	 *
	 */
	public void exportFinished(Map<CmsObjectType, Integer> summary);
}