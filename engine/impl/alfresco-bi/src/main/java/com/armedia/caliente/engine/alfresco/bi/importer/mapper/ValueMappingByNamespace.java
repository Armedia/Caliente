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

	protected CmfAttribute<CmfValue> locateAttribute(String baseName, CmfObject<CmfValue> object) {
		// First, get the source attribute's namespace
		for (String srcNs : this.sourceValues) {
			String srcAttName = String.format("%s:%s", srcNs, baseName);
			CmfAttribute<CmfValue> srcAtt = findMatchingAttribute(srcAttName, object);
			if (srcAtt != null) { return srcAtt; }
		}
		return null;
	}

	@Override
	public String getValue(SchemaAttribute tgtAtt, CmfObject<CmfValue> object) {
		Objects.requireNonNull(tgtAtt, "Must provide a target attribute to map for");
		Objects.requireNonNull(object, "Must provide a source object to map against");
		// First, get the source attribute's namespace
		Matcher m = ValueMappingByNamespace.NSPARSER.matcher(tgtAtt.name);
		if (!m.matches()) {
			// No namespace!! Not applicable
			return null;
		}

		final String tgtNs = m.group(1);
		if (!StringUtils.equals(tgtNs, this.target)) { return null; }

		return generateValue(locateAttribute(m.group(2), object));
	}
}