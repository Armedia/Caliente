package com.armedia.caliente.engine.transform;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.caliente.store.CmfAttributeMapper;
import com.armedia.caliente.store.CmfType;

public class ImmutableTransformationContext implements TransformationContext {

	private static class ImmutableObjectDataMember extends ObjectDataMember {

		public ImmutableObjectDataMember(ObjectDataMember pattern) {
			super(pattern);
		}

		@Override
		public List<Object> getValues() {
			return Collections.unmodifiableList(super.getValues());
		}

		@Override
		public ObjectDataMember setValue(Object value) {
			throw ImmutableTransformationContext.fail();
		}

		@Override
		public ObjectDataMember setValues(Iterator<?> values) {
			throw ImmutableTransformationContext.fail();
		}

	}

	private static class ImmutableObjectData extends ObjectData {

		private final ObjectData object;
		private final Map<String, ObjectDataMember> att;
		private final Map<String, ObjectDataMember> priv;
		private final Set<String> originalDecorators;
		private final Set<String> decorators;

		private ImmutableObjectData(ObjectData object) {
			this.object = object;
			this.originalDecorators = Collections.unmodifiableSet(object.getOriginalDecorators());
			this.decorators = Collections.unmodifiableSet(object.getDecorators());

			Map<String, ObjectDataMember> orig = null;
			Map<String, ObjectDataMember> copy = null;

			orig = object.getAtt();
			copy = new TreeMap<>();
			for (String s : orig.keySet()) {
				copy.put(s, new ImmutableObjectDataMember(orig.get(s)));
			}
			this.att = Collections.unmodifiableMap(new LinkedHashMap<>(copy));

			orig = object.getPriv();
			copy = new TreeMap<>();
			for (String s : orig.keySet()) {
				copy.put(s, new ImmutableObjectDataMember(orig.get(s)));
			}
			this.priv = Collections.unmodifiableMap(new LinkedHashMap<>(copy));
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
		public String getOriginalSubtype() {
			return this.object.getOriginalSubtype();
		}

		@Override
		public ImmutableObjectData setSubtype(String subtype) {
			throw ImmutableTransformationContext.fail();
		}

		@Override
		public Set<String> getOriginalDecorators() {
			return this.originalDecorators;
		}

		@Override
		public Set<String> getDecorators() {
			return this.decorators;
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
		public ImmutableObjectData setName(String name) {
			throw ImmutableTransformationContext.fail();
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
		public Map<String, ObjectDataMember> getAtt() {
			return this.att;
		}

		@Override
		public Map<String, ObjectDataMember> getPriv() {
			return this.priv;
		}

		@Override
		public String getOriginalName() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private static class ImmutableAttributeMapper extends CmfAttributeMapper {
		private final CmfAttributeMapper mapper;

		private ImmutableAttributeMapper(CmfAttributeMapper mapper) {
			this.mapper = mapper;
		}

		@Override
		protected Mapping createMapping(CmfType objectType, String mappingName, String sourceValue,
			String targetValue) {
			throw ImmutableTransformationContext.fail();
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

	private final ObjectData objectData;
	private final Map<String, ObjectDataMember> variables;
	private final CmfAttributeMapper attributeMapper;

	public ImmutableTransformationContext(TransformationContext context) {
		this.objectData = new ImmutableObjectData(context.getObject());
		this.variables = Collections.unmodifiableMap(context.getVariables());
		this.attributeMapper = new ImmutableAttributeMapper(context.getAttributeMapper());
	}

	private static UnsupportedOperationException fail() {
		return new UnsupportedOperationException("This view of the transformation context is immutable");
	}

	@Override
	public ObjectData getObject() {
		return this.objectData;
	}

	@Override
	public Map<String, ObjectDataMember> getVariables() {
		return this.variables;
	}

	@Override
	public CmfAttributeMapper getAttributeMapper() {
		return this.attributeMapper;
	}
}