package com.armedia.caliente.engine.transform;

import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.store.CmfAttributeMapper;

public class DefaultTransformationContext implements TransformationContext {

	private final ObjectData objectData;
	private final CmfAttributeMapper mapper;

	private final Map<String, ObjectDataMember> variables = new TreeMap<>();

	public DefaultTransformationContext(ObjectData objectData, CmfAttributeMapper mapper) {
		this.objectData = objectData;
		this.mapper = mapper;
	}

	@Override
	public ObjectData getObject() {
		return this.objectData;
	}

	@Override
	public Map<String, ObjectDataMember> getVariables() {
		return this.variables;
	}

	@Override
	public CmfAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

}