package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

class ValueMappingByNamespace extends ValueMapping {
	private static final Pattern NSPARSER = Pattern.compile("^([^:]+):(.+)$");

	ValueMappingByNamespace(Mapping m) {
		super(m);
	}

	protected String getSourceAttributeName(String baseName, CmfObject<CmfValue> object) {
		// Go through each of our namespace mappings, and try to find an attribute with
		// the given baseName within that namespace
		for (String srcNs : this.sourceValues) {
			String srcAtt = findSourceAttributeName(String.format("%s:%s", srcNs, baseName), object);
			// If we found one, return it immediately!
			if (srcAtt != null) { return srcAtt; }
		}
		return null;
	}

	@Override
	public MappedValue getValue(SchemaAttribute tgtAtt, CmfObject<CmfValue> object) {
		Objects.requireNonNull(tgtAtt, "Must provide a target attribute to map for");
		Objects.requireNonNull(object, "Must provide a source object to map against");
		// First, get the source attribute's namespace
		Matcher m = ValueMappingByNamespace.NSPARSER.matcher(tgtAtt.name);
		if (!m.matches()) {
			// No namespace!! Not applicable
			return null;
		}

		// Is the attribute's namespace the same as we're configured for?
		if (!StringUtils.equals(m.group(1), this.target)) { return null; }

		// Ok...find a candidate from one of our namespaces
		final String attribute = getSourceAttributeName(m.group(2), object);
		if (attribute == null) { return null; }
		return new MappedValue() {
			private final String attributeName = attribute;
			private final char separator = ValueMappingByNamespace.this.separator;

			@Override
			public String render(CmfObject<CmfValue> object) {
				CmfAttribute<CmfValue> attribute = object.getAttribute(this.attributeName);
				if (attribute == null) { return null; }
				return ValueMapping.generateValue(this.separator, attribute);
			}
		};
	}
}