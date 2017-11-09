package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public interface MappedValue {

	public String render(CmfObject<CmfValue> object);

}