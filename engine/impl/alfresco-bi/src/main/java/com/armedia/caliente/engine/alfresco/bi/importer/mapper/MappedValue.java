package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.Properties;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public interface MappedValue {

	public void render(Properties properties, CmfObject<CmfValue> object);

}