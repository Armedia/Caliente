package com.armedia.caliente.engine.transform;

import java.util.Map;
import java.util.TreeMap;

import javax.script.Bindings;
import javax.script.ScriptContext;

import com.armedia.caliente.engine.xml.Expression.ScriptContextConfig;
import com.armedia.caliente.store.CmfValueMapper;

public class TransformationContext implements ScriptContextConfig {

	private final TransformableObject object;
	private final CmfValueMapper mapper;

	private final Map<String, TypedValue> variables;

	public TransformationContext(TransformableObject object, CmfValueMapper mapper) {
		this(object, mapper, null);
	}

	protected TransformationContext(TransformableObject object, CmfValueMapper mapper,
		Map<String, TypedValue> variables) {
		this.object = object;
		this.mapper = mapper;
		if (variables == null) {
			variables = new TreeMap<>();
		}
		this.variables = variables;
	}

	public TransformableObject getObject() {
		return this.object;
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
		bindings.put("obj", getObject());
		bindings.put("vars", getVariables());
		bindings.put("mapper", getAttributeMapper());
	}

}