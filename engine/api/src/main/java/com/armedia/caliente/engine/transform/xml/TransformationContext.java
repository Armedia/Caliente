package com.armedia.caliente.engine.transform.xml;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValueCodec;

public interface TransformationContext<V> {

	public CmfObject<V> getObject();

	public CmfValueCodec<V> getCodec(CmfDataType type);

}