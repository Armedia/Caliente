package com.armedia.caliente.engine.transform;

import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.store.CmfAttributeMapper;

public class TransformationContext {

	private final TransformableObjectFacade object;
	private final CmfAttributeMapper mapper;

	private final Map<String, TypedValue> variables;

	public TransformationContext(TransformableObjectFacade object, CmfAttributeMapper mapper) {
		this(object, mapper, null);
	}

	protected TransformationContext(TransformableObjectFacade object, CmfAttributeMapper mapper,
		Map<String, TypedValue> variables) {
		this.object = object;
		this.mapper = mapper;
		if (variables == null) {
			variables = new TreeMap<>();
		}
		this.variables = variables;
	}

	public TransformableObjectFacade getObject() {
		return this.object;
	}

	public Map<String, TypedValue> getVariables() {
		return this.variables;
	}

	public CmfAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

}