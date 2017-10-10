package com.armedia.caliente.engine.transform;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.script.Bindings;
import javax.script.ScriptContext;

import com.armedia.caliente.engine.extmeta.ExternalMetadataException;
import com.armedia.caliente.engine.extmeta.ExternalMetadataLoader;
import com.armedia.caliente.engine.xml.Expression.ScriptContextConfig;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class TransformationContext implements ScriptContextConfig {

	private final CmfObject<CmfValue> baseObject;
	private final TransformableObject transformableObject;
	private final CmfValueMapper mapper;
	private final ExternalMetadataLoader metadataLoader;

	private final Map<String, TypedValue> variables;

	public TransformationContext(CmfObject<CmfValue> baseObject, TransformableObject transformableObject,
		CmfValueMapper mapper, ExternalMetadataLoader metadataLoader) {
		this(baseObject, transformableObject, mapper, metadataLoader, null);
	}

	protected TransformationContext(CmfObject<CmfValue> baseObject, TransformableObject transformableObject,
		CmfValueMapper mapper, ExternalMetadataLoader metadataLoader, Map<String, TypedValue> variables) {
		this.baseObject = baseObject;
		this.transformableObject = transformableObject;
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

	public TransformableObject getTransformableObject() {
		return this.transformableObject;
	}

	public Map<String, TypedValue> getVariables() {
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