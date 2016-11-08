package com.armedia.caliente.store;

public interface CmfNameFixer<V> {

	public boolean supportsType(CmfType type);

	public String fixName(CmfObject<V> dataObject) throws CmfStorageException;

	public void nameFixed(CmfObject<V> dataObject, String oldName, String newName);

	public boolean handleException(Exception e);
}