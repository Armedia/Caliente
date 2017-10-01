package com.armedia.caliente.engine.transform;

import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.store.CmfAttributeMapper;

public class TransformationContext {

	private final TransformableObjectFacade transformableObjectFacade;
	private final CmfAttributeMapper mapper;

	private final Map<String, TypedValue> variables;

	public TransformationContext(TransformableObjectFacade transformableObjectFacade, CmfAttributeMapper mapper) {
		this(transformableObjectFacade, mapper, null);
	}

	protected TransformationContext(TransformableObjectFacade transformableObjectFacade, CmfAttributeMapper mapper,
		Map<String, TypedValue> variables) {
		this.transformableObjectFacade = transformableObjectFacade;
		this.mapper = mapper;
		if (variables == null) {
			variables = new TreeMap<>();
		}
		this.variables = variables;
	}

	public final TransformableObjectFacade getObject() {
		return this.transformableObjectFacade;
	}

	public final Map<String, TypedValue> getVariables() {
		return this.variables;
	}

	public final CmfAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

}