/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.script.Bindings;
import javax.script.ScriptContext;

import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataException;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValueMapper;

public class DynamicElementContext<VALUE> implements Consumer<ScriptContext> {

	private final CmfObject<VALUE> baseObject;
	private final DynamicObject dynamicObject;
	private final CmfValueMapper mapper;
	private final ExternalMetadataLoader metadataLoader;

	private final Map<String, DynamicValue> variables;

	public DynamicElementContext(CmfObject<VALUE> baseObject, DynamicObject dynamicObject, CmfValueMapper mapper,
		ExternalMetadataLoader metadataLoader) {
		this(baseObject, dynamicObject, mapper, metadataLoader, null);
	}

	protected DynamicElementContext(CmfObject<VALUE> baseObject, DynamicObject dynamicObject, CmfValueMapper mapper,
		ExternalMetadataLoader metadataLoader, Map<String, DynamicValue> variables) {
		this.baseObject = baseObject;
		this.dynamicObject = dynamicObject;
		this.mapper = mapper;
		if (variables == null) {
			variables = new TreeMap<>();
		}
		this.variables = variables;
		this.metadataLoader = metadataLoader;
	}

	public CmfObject<VALUE> getBaseObject() {
		return this.baseObject;
	}

	protected ExternalMetadataLoader getMetadataLoader() {
		return this.metadataLoader;
	}

	public DynamicObject getDynamicObject() {
		return this.dynamicObject;
	}

	public Map<String, DynamicValue> getVariables() {
		return this.variables;
	}

	public CmfValueMapper getAttributeMapper() {
		return this.mapper;
	}

	public Map<String, CmfAttribute<VALUE>> getAttributeValues(CmfObject<VALUE> object, Collection<String> sourceNames)
		throws ExternalMetadataException {
		if (this.metadataLoader == null) { return new HashMap<>(); }
		return this.metadataLoader.getAttributeValues(object, sourceNames);
	}

	public Map<String, CmfAttribute<VALUE>> getAttributeValues(CmfObject<VALUE> object, String sourceName)
		throws ExternalMetadataException {
		if (this.metadataLoader == null) { return new HashMap<>(); }
		return this.metadataLoader.getAttributeValues(object, sourceName);
	}

	@Override
	public void accept(ScriptContext ctx) {
		final Bindings bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put("baseObj", getBaseObject());
		bindings.put("obj", getDynamicObject());
		bindings.put("vars", getVariables());
		bindings.put("mapper", getAttributeMapper());
	}

}