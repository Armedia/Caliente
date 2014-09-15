package com.delta.cmsmf.cms;

import java.util.Map;
import java.util.Set;

import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public interface CmsTransferContext {

	public String getRootObjectId();

	public IDfSession getSession();

	public CmsAttributeMapper getAttributeMapper();

	public IDfValue getValue(String name);

	public IDfValue setValue(String name, IDfValue value);

	public IDfValue clearValue(String name);

	public boolean hasValue(String name);

	public <T extends IDfPersistentObject, O extends CmsObject<T>> Map<String, O> retrieveObjects(Class<O> klass,
		Set<String> ids) throws CMSMFException;
}