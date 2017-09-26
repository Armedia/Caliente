package com.armedia.caliente.engine.transform.xml;

import java.util.Set;

import com.armedia.caliente.store.CmfObject;

public interface TransformationContext<V> {

	public CmfObject<V> getObject();

	public String getCurrentSubtype();

	public void setCurrentSubtype();

	public Set<String> getCurrentDecorators();

}