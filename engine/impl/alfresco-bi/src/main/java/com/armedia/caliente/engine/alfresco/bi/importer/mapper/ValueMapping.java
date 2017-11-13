package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

class ValueMapping {
	protected final String target;
	protected final Set<String> sourceValues;
	protected final char separator;
	protected final boolean caseSensitive;

	ValueMapping(Mapping mapping) {
		this.target = StringUtils.strip(mapping.getTgt());
		this.caseSensitive = mapping.isCaseSensitive();
		this.separator = mapping.getSeparator();
		String value = StringUtils.strip(mapping.getValue());
		Set<String> sourceValues = new LinkedHashSet<>();
		for (String v : Tools.splitEscaped(this.separator, value)) {
			sourceValues.add(v);
		}
		this.sourceValues = Tools.freezeSet(sourceValues);
	}

	protected String findSourceAttributeName(String srcAttName, CmfObject<CmfValue> object) {
		if (this.caseSensitive) { return (object.hasAttribute(srcAttName) ? srcAttName : null); }
		for (String n : object.getAttributeNames()) {
			if (StringUtils.equalsIgnoreCase(srcAttName, n)) { return n; }
		}
		return null;
	}

	private String findSourceAttributeName(CmfObject<CmfValue> object) {
		for (String candidate : this.sourceValues) {
			String srcAtt = findSourceAttributeName(candidate, object);
			if (srcAtt != null) { return srcAtt; }
		}
		return null;
	}

	protected static String generateValue(char separator, CmfAttribute<CmfValue> srcAtt) {
		if (srcAtt == null) { return null; }
		if (!srcAtt.hasValues()) { return StringUtils.EMPTY; }
		List<String> values = new ArrayList<>(srcAtt.getValueCount());
		for (CmfValue value : srcAtt) {
			// Avoid null values
			if (value.isNull()) {
				continue;
			}
			values.add(value.asString());
		}
		return Tools.joinEscaped(separator, values);
	}

	public MappedValue getValue(SchemaAttribute tgtAtt, CmfObject<CmfValue> object) {
		Objects.requireNonNull(tgtAtt, "Must provide a target attribute to map for");
		Objects.requireNonNull(object, "Must provide a source object to map against");
		if (!StringUtils.equals(this.target, tgtAtt.name)) { return null; }

		final String attribute = findSourceAttributeName(object);
		if (attribute == null) { return null; }

		return new MappedValue() {
			private final String attributeName = attribute;
			private final char separator = ValueMapping.this.separator;

			@Override
			public boolean render(Properties properties, CmfObject<CmfValue> object) {
				CmfAttribute<CmfValue> attribute = object.getAttribute(this.attributeName);
				if (attribute == null) { return false; }
				String value = ValueMapping.generateValue(this.separator, attribute);
				properties.setProperty(this.attributeName, value);
				return true;
			}
		};
	}
}