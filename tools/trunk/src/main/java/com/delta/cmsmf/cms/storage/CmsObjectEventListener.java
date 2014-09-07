package com.delta.cmsmf.cms.storage;

import java.util.Map;

import com.delta.cmsmf.cms.CmsCounter;
import com.delta.cmsmf.cms.CmsObjectType;

public interface CmsObjectEventListener {

	public void processingStarted(int totalObjects);

	public void objectTypeStarted(CmsObjectType objectType, int totalObjects);

	public void objectProcessingStarted(CmsObjectType objectType, String objectId);

	public void objectProcessed(CmsObjectType objectType, String objectId, CmsCounter.Result result);

	public void objectTypeConcluded(CmsObjectType objectType, Map<CmsCounter.Result, Integer> counters);

	public void processingConcluded(Map<CmsCounter.Result, Integer> counters);
}