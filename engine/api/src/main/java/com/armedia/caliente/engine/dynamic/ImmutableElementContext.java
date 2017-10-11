package com.armedia.caliente.engine.dynamic;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;
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
		public CmfType getType() {
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
		public String getProductName() {
			return this.object.getProductName();
		}

		@Override
		public String getProductVersion() {
			return this.object.getProductVersion();
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
		protected Mapping createMapping(CmfType objectType, String mappingName, String sourceValue,
			String targetValue) {
			throw ImmutableElementContext.fail();
		}

		@Override
		public Mapping getTargetMapping(CmfType objectType, String mappingName, String sourceValue) {
			return this.mapper.getTargetMapping(objectType, mappingName, sourceValue);
		}

		@Override
		public Mapping getSourceMapping(CmfType objectType, String mappingName, String targetValue) {
			return this.mapper.getSourceMapping(objectType, mappingName, targetValue);
		}

		@Override
		public Map<CmfType, Set<String>> getAvailableMappings() {
			return this.mapper.getAvailableMappings();
		}

		@Override
		public Set<String> getAvailableMappings(CmfType objectType) {
			return this.mapper.getAvailableMappings(objectType);
		}

		@Override
		public Map<String, String> getMappings(CmfType objectType, String mappingName) {
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