package com.armedia.caliente.store;

public interface CmfTransformer {

	public CmfObject<CmfValue> transform(CmfObject<CmfValue> object) throws CmfStorageException;

	public void close();
}