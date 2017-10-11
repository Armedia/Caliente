package com.armedia.caliente.engine.dynamic;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.script.Bindings;
import javax.script.ScriptContext;

import com.armedia.caliente.engine.dynamic.extmeta.ExternalMetadataException;
import com.armedia.caliente.engine.dynamic.extmeta.ExternalMetadataLoader;
import com.armedia.caliente.engine.dynamic.xml.Expression.ScriptContextConfig;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class DynamicElementContext implements ScriptContextConfig {

	private final CmfObject<CmfValue> baseObject;
	private final DynamicObject dynamicObject;
	private final CmfValueMapper mapper;
	private final ExternalMetadataLoader metadataLoader;

	private final Map<String, DynamicValue> variables;

	public DynamicElementContext(CmfObject<CmfValue> baseObject, DynamicObject dynamicObject,
		CmfValueMapper mapper, ExternalMetadataLoader metadataLoader) {
		this(baseObject, dynamicObject, mapper, metadataLoader, null);
	}

	protected DynamicElementContext(CmfObject<CmfValue> baseObject, DynamicObject dynamicObject,
		CmfValueMapper mapper, ExternalMetadataLoader metadataLoader, Map<String, DynamicValue> variables) {
		this.baseObject = baseObject;
		this.dynamicObject = dynamicObject;
		this.mapper = mapper;
		if (variables == null) {
			variables = new TreeMap<>();
		}
		this.variables = variables;
		this.metadataLoader = metadataLoader;
	}

	public CmfObject<CmfValue> getBaseObject() {
		return this.baseObject;
	}

	public ExternalMetadataLoader getMetadataLoader() {
		return this.metadataLoader;
	}

	public DynamicObject getTransformableObject() {
		return this.dynamicObject;
	}

	public Map<String, DynamicValue> getVariables() {
		return this.variables;
	}

	public CmfValueMapper getAttributeMapper() {
		return this.mapper;
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object, Collection<String> sourceNames)
		throws ExternalMetadataException {
		return this.metadataLoader.getAttributeValues(object, sourceNames);
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object, String sourceName)
		throws ExternalMetadataException {
		return this.metadataLoader.getAttributeValues(object, sourceName);
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