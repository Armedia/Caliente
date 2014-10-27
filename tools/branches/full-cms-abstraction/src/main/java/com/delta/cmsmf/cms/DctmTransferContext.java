package com.delta.cmsmf.cms;

import java.util.Set;

import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public interface DctmTransferContext {

	public String getRootObjectId();

	public IDfSession getSession();

	public StoredAttributeMapper getAttributeMapper();

	public IDfValue getValue(String name);

	public IDfValue setValue(String name, IDfValue value);

	public IDfValue clearValue(String name);

	public boolean hasValue(String name);

	public void deserializeObjects(Class<? extends DctmPersistentObject<?>> klass, Set<String> ids,
		StoredObjectHandler<IDfValue> handler) throws CMSMFException;

	public ContentStreamStore getContentStreamStore();

	public void printf(String format, Object... args);
}