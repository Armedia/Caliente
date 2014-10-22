package com.delta.cmsmf.cms.storage;

import java.util.Set;

public interface CmsTransferContext {
	public String getRootObjectId();

	public CmsAttributeMapper getAttributeMapper();

	public Set<String> getPropertyNames();

	public CmsProperty getProperty(String name);

	public CmsProperty setProperty(String name, CmsValue<?>... value);

	public CmsProperty clearProperty(String name);

	public boolean hasProperty(String name);

	public CmsFileSystem getFileSystem();

	public void printf(String format, Object... args);
}