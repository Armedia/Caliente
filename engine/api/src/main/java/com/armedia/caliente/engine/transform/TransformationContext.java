package com.armedia.caliente.engine.transform;

import java.util.Map;
import java.util.TreeMap;

import javax.script.Bindings;
import javax.script.ScriptContext;

import com.armedia.caliente.engine.xml.Expression.ScriptContextConfig;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class TransformationContext implements ScriptContextConfig {

	private final CmfObject<CmfValue> baseObject;
	private final TransformableObject transformableObject;
	private final CmfValueMapper mapper;

	private final Map<String, TypedValue> variables;

	public TransformationContext(CmfObject<CmfValue> baseObject, TransformableObject transformableObject,
		CmfValueMapper mapper) {
		this(baseObject, transformableObject, mapper, null);
	}

	protected TransformationContext(CmfObject<CmfValue> baseObject, TransformableObject transformableObject,
		CmfValueMapper mapper, Map<String, TypedValue> variables) {
		this.baseObject = baseObject;
		this.transformableObject = transformableObject;
		this.mapper = mapper;
		if (variables == null) {
			variables = new TreeMap<>();
		}
		this.variables = variables;
	}

	public CmfObject<CmfValue> getBaseObject() {
		return this.baseObject;
	}

	public TransformableObject getTransformableObject() {
		return this.transformableObject;
	}

	public Map<String, TypedValue> getVariables() {
		return this.variables;
	}

	public CmfValueMapper getAttributeMapper() {
		return this.mapper;
	}

	@Override
	public void configure(ScriptContext ctx) {
		final Bindings bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put("baseObj", getBaseObject());
		bindings.put("obj", getTransformableObject());
		bindings.put("vars", getVariables());
		bindings.put("mapper", getAttributeMapper());
	}

}