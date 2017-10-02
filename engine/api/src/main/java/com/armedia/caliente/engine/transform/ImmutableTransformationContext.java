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
import com.armedia.commons.utilities.Tools;

public class ImmutableTransformationContext extends TransformationContext {

	private static class ImmutableTypedValue extends TypedValue {

		public ImmutableTypedValue(TypedValue pattern) {
			super(pattern);
		}

		@Override
		public List<Object> getValues() {
			return Collections.unmodifiableList(super.getValues());
		}

		@Override
		public TypedValue setValue(Object value) {
			throw ImmutableTransformationContext.fail();
		}

		@Override
		public TypedValue setValues(Iterator<?> values) {
			throw ImmutableTransformationContext.fail();
		}

	}

	private static class ImmutableObjectData extends TransformableObject {

		private final TransformableObject object;
		private final Map<String, TypedValue> att;
		private final Map<String, TypedValue> priv;
		private final Set<String> originalDecorators;
		private final Set<String> decorators;

		private ImmutableObjectData(TransformableObject object) {
			this.object = object;
			this.originalDecorators = Collections.unmodifiableSet(object.getOriginalDecorators());
			this.decorators = Collections.unmodifiableSet(object.getDecorators());
			this.att = ImmutableTransformationContext.makeImmutable(object.getAtt());
			this.priv = ImmutableTransformationContext.makeImmutable(object.getPriv());
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
		public Map<String, TypedValue> getAtt() {
			return this.att;
		}

		@Override
		public Map<String, TypedValue> getPriv() {
			return this.priv;
		}

		@Override
		public String getOriginalName() {
			return this.object.getOriginalName();
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

	public ImmutableTransformationContext(TransformationContext context) {
		super(new ImmutableObjectData(context.getObject()), new ImmutableAttributeMapper(context.getAttributeMapper()),
			ImmutableTransformationContext.makeImmutable(context.getVariables()));
	}

	private static Map<String, TypedValue> makeImmutable(Map<String, TypedValue> orig) {
		if ((orig == null) || orig.isEmpty()) { return Collections.emptyMap(); }
		Map<String, TypedValue> copy = new TreeMap<>();
		for (String s : orig.keySet()) {
			copy.put(s, new ImmutableTypedValue(orig.get(s)));
		}
		return Tools.freezeMap(new LinkedHashMap<>(copy));

	}

	private static UnsupportedOperationException fail() {
		return new UnsupportedOperationException("This view of the transformation context is immutable");
	}
}