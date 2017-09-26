package com.armedia.caliente.store;

public interface CmfTransformer {

	public <V> CmfObject<V> transformObject(CmfObject<V> object);

	public void close();

}