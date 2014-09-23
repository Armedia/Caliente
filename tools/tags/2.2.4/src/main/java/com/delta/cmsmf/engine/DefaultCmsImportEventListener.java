package com.delta.cmsmf.engine;

import java.util.Map;

import com.delta.cmsmf.cms.CmsImportResult;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;

public class DefaultCmsImportEventListener implements CmsImportEngineListener {

	@Override
	public void importStarted(Map<CmsObjectType, Integer> summary) {
	}

	@Override
	public void objectTypeImportStarted(CmsObjectType objectType, int totalObjects) {
	}

	@Override
	public void objectImportStarted(CmsObject<?> object) {
	}

	@Override
	public void objectImportCompleted(CmsObject<?> object, CmsImportResult cmsImportResult) {
	}

	@Override
	public void objectImportFailed(CmsObject<?> object, Throwable thrown) {
	}

	@Override
	public void objectTypeImportFinished(CmsObjectType objectType, Map<CmsImportResult, Integer> counters) {
	}

	@Override
	public void importFinished(Map<CmsImportResult, Integer> counters) {
	}
}