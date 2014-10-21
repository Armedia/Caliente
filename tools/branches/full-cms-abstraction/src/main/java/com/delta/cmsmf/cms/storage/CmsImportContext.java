package com.delta.cmsmf.cms.storage;

import java.util.Set;

import com.delta.cmsmf.cms.storage.CmsObjectStore.ObjectHandler;
import com.delta.cmsmf.exception.CMSMFException;

public interface CmsImportContext extends CmsTransferContext, CmsImportListener {

	public void deserializeObjects(CmsObjectType type, Set<String> ids, ObjectHandler handler) throws CMSMFException;

}