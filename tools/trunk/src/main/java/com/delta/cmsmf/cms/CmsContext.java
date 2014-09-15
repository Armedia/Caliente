package com.delta.cmsmf.cms;

import java.util.List;
import java.util.Map;

import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;

public interface CmsContext {

	public IDfSession getSession();

	public CmsAttributeMapper getAttributeMapper();

	public <T extends IDfPersistentObject, O extends CmsObject<T>> Map<String, O> getDependentObjects(Class<O> klass,
		List<String> ids) throws CMSMFException;
}