package com.armedia.caliente.engine.transform;

import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.store.CmfAttributeMapper;

public class TransformationContext {

	private final ObjectData objectData;
	private final CmfAttributeMapper mapper;

	private final Map<String, TypedValue> variables;

	public TransformationContext(ObjectData objectData, CmfAttributeMapper mapper) {
		this(objectData, mapper, null);
	}

	protected TransformationContext(ObjectData objectData, CmfAttributeMapper mapper,
		Map<String, TypedValue> variables) {
		this.objectData = objectData;
		this.mapper = mapper;
		if (variables == null) {
			variables = new TreeMap<>();
		}
		this.variables = variables;
	}

	public final ObjectData getObject() {
		return this.objectData;
	}

	public final Map<String, TypedValue> getVariables() {
		return this.variables;
	}

	public final CmfAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

}