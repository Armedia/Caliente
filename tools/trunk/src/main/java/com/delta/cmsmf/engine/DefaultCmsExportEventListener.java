package com.delta.cmsmf.engine;

import java.util.Map;

import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;

public class DefaultCmsExportEventListener implements CmsExportEventListener {

	@Override
	public void exportStarted(String dql) {
	}

	@Override
	public void objectExportStarted(CmsObjectType objectType, String objectId) {
	}

	@Override
	public void objectExportCompleted(CmsObject<?> object) {
	}

	@Override
	public void objectSkipped(CmsObjectType objectType, String objectId) {
	}

	@Override
	public void objectExportFailed(CmsObjectType objectType, String objectId, Throwable thrown) {
	}

	@Override
	public void exportFinished(Map<CmsObjectType, Integer> summary) {
	}
}