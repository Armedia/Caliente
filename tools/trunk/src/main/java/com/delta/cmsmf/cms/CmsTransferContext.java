package com.delta.cmsmf.cms;

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
}