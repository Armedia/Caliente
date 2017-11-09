package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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

	protected CmfAttribute<CmfValue> findMatchingAttribute(String srcAttName, CmfObject<CmfValue> object) {
		if (this.caseSensitive) { return object.getAttribute(srcAttName); }
		for (String n : object.getAttributeNames()) {
			if (StringUtils.equalsIgnoreCase(srcAttName, n)) { return object.getAttribute(n); }
		}
		return null;
	}

	private CmfAttribute<CmfValue> findMatchingAttribute(CmfObject<CmfValue> object) {
		for (String candidate : this.sourceValues) {
			CmfAttribute<CmfValue> srcAtt = findMatchingAttribute(candidate, object);
			if (srcAtt != null) { return srcAtt; }
		}
		return null;
	}

	protected final String generateValue(CmfAttribute<CmfValue> srcAtt) {
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
		return Tools.joinEscaped(this.separator, values);
	}

	public String getValue(SchemaAttribute tgtAtt, CmfObject<CmfValue> object) {
		Objects.requireNonNull(tgtAtt, "Must provide a target attribute to map for");
		Objects.requireNonNull(object, "Must provide a source object to map against");
		if (!StringUtils.equals(this.target, tgtAtt.name)) { return null; }
		return generateValue(findMatchingAttribute(object));
	}
}