package com.armedia.caliente.store;

public interface CmfTransformer {

	public CmfObject<CmfValue> transform(CmfAttributeMapper mapper, CmfObject<CmfValue> object)
		throws CmfStorageException;

	public void close();
}