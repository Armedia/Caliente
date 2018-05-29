package com.armedia.caliente.store;

public interface CmfNameFixer<VALUE> {

	public boolean supportsType(CmfType type);

	public String fixName(CmfObject<VALUE> dataObject) throws CmfStorageException;

	public void nameFixed(CmfObject<VALUE> dataObject, String oldName, String newName);

	public boolean handleException(Exception e);
}