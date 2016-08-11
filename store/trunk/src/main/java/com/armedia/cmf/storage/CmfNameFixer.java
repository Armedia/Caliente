package com.armedia.cmf.storage;

public interface CmfNameFixer<V> {

	public String fixName(CmfObject<V> dataObject) throws CmfStorageException;

	public boolean handleException(Exception e);
}