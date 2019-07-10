/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.commons.utilities.Tools;

public class ImmutableElementContext extends DynamicElementContext {

	private static class ImmutableDynamicValue extends DynamicValue {

		public ImmutableDynamicValue(DynamicValue pattern) {
			super(pattern);
		}

		@Override
		public List<Object> getValues() {
			return Collections.unmodifiableList(super.getValues());
		}

		@Override
		public DynamicValue setValue(Object value) {
			throw ImmutableElementContext.fail();
		}

		@Override
		public DynamicValue setValues(Iterator<?> values) {
			throw ImmutableElementContext.fail();
		}

	}

	private static class ImmutableDynamicObject extends DynamicObject {

		private final DynamicObject object;
		private final Map<String, DynamicValue> att;
		private final Map<String, DynamicValue> priv;
		private final Set<String> secondaries;

		private ImmutableDynamicObject(DynamicObject object) {
			this.object = object;
			this.secondaries = Collections.unmodifiableSet(object.getSecondarySubtypes());
			this.att = ImmutableElementContext.makeImmutable(object.getAtt());
			this.priv = ImmutableElementContext.makeImmutable(object.getPriv());
		}

		@Override
		public String getObjectId() {
			return this.object.getObjectId();
		}

		@Override
		public String getHistoryId() {
			return this.object.getHistoryId();
		}

		@Override
		public boolean isHistoryCurrent() {
			return this.object.isHistoryCurrent();
		}

		@Override
		public CmfObject.Archetype getType() {
			return this.object.getType();
		}

		@Override
		public String getLabel() {
			return this.object.getLabel();
		}

		@Override
		public String getOriginalSubtype() {
			return this.object.getOriginalSubtype();
		}

		@Override
		public ImmutableDynamicObject setSubtype(String subtype) {
			throw ImmutableElementContext.fail();
		}

		@Override
		public Set<String> getOriginalSecondarySubtypes() {
			return this.object.getOriginalSecondarySubtypes();
		}

		@Override
		public Set<String> getSecondarySubtypes() {
			return this.secondaries;
		}

		@Override
		public int getDependencyTier() {
			return this.object.getDependencyTier();
		}

		@Override
		public String getName() {
			return this.object.getName();
		}

		@Override
		public ImmutableDynamicObject setName(String name) {
			throw ImmutableElementContext.fail();
		}

		@Override
		public Map<String, DynamicValue> getAtt() {
			return this.att;
		}

		@Override
		public Map<String, DynamicValue> getPriv() {
			return this.priv;
		}

		@Override
		public String getOriginalName() {
			return this.object.getOriginalName();
		}

	}

	private static class ImmutableAttributeMapper extends CmfValueMapper {
		private final CmfValueMapper mapper;

		private ImmutableAttributeMapper(CmfValueMapper mapper) {
			this.mapper = mapper;
		}

		@Override
		protected Mapping createMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue,
			String targetValue) {
			throw ImmutableElementContext.fail();
		}

		@Override
		public Mapping getTargetMapping(CmfObject.Archetype objectType, String mappingName, String sourceValue) {
			return this.mapper.getTargetMapping(objectType, mappingName, sourceValue);
		}

		@Override
		public Collection<Mapping> getSourceMapping(CmfObject.Archetype objectType, String mappingName,
			String targetValue) {
			return this.mapper.getSourceMapping(objectType, mappingName, targetValue);
		}

		@Override
		public Map<CmfObject.Archetype, Set<String>> getAvailableMappings() {
			return this.mapper.getAvailableMappings();
		}

		@Override
		public Set<String> getAvailableMappings(CmfObject.Archetype objectType) {
			return this.mapper.getAvailableMappings(objectType);
		}

		@Override
		public Map<String, String> getMappings(CmfObject.Archetype objectType, String mappingName) {
			return this.mapper.getMappings(objectType, mappingName);
		}
	}

	public ImmutableElementContext(CmfObject<CmfValue> baseObject, DynamicObject dynamicObject, CmfValueMapper mapper,
		ExternalMetadataLoader metadataLoader, Map<String, DynamicValue> variables) {
		super(baseObject, new ImmutableDynamicObject(dynamicObject), new ImmutableAttributeMapper(mapper),
			metadataLoader, ImmutableElementContext.makeImmutable(variables));
	}

	public ImmutableElementContext(DynamicElementContext context) {
		super(context.getBaseObject(), new ImmutableDynamicObject(context.getDynamicObject()),
			new ImmutableAttributeMapper(context.getAttributeMapper()), context.getMetadataLoader(),
			ImmutableElementContext.makeImmutable(context.getVariables()));
	}

	private static Map<String, DynamicValue> makeImmutable(Map<String, DynamicValue> orig) {
		if ((orig == null) || orig.isEmpty()) { return Collections.emptyMap(); }
		Map<String, DynamicValue> copy = new TreeMap<>();
		for (String s : orig.keySet()) {
			copy.put(s, new ImmutableDynamicValue(orig.get(s)));
		}
		return Tools.freezeMap(new LinkedHashMap<>(copy));

	}

	private static UnsupportedOperationException fail() {
		return new UnsupportedOperationException("This view of the transformation context is immutable");
	}
}