package com.armedia.caliente.engine.transform.xml;

import com.armedia.caliente.store.CmfObject;

public interface TransformationContext<V> {

	public CmfObject<V> getObject();

}