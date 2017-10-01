package com.armedia.caliente.engine.transform;

import java.util.Map;

import com.armedia.caliente.store.CmfAttributeMapper;

public interface TransformationContext {

	public ObjectData getObject();

	public Map<String, ObjectDataMember> getVariables();

	public CmfAttributeMapper getAttributeMapper();

}