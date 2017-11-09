package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

class ValueMappingByNamespace extends ValueMapping {
	private static final Pattern NSPARSER = Pattern.compile("^([^:]+):(.+)$");

	ValueMappingByNamespace(Mapping m) {
		super(m);
	}

	@Override
	protected CmfAttribute<CmfValue> findMatchingAttribute(String tgtAttName, CmfObject<CmfValue> object) {
		// First, get the source attribute's namespace
		Matcher m = ValueMappingByNamespace.NSPARSER.matcher(tgtAttName);
		if (!m.matches()) {
			// No namespace!! Not applicable
			return null;
		}

		final String tgtNs = m.group(1);
		if (this.caseSensitive) {
			if (!StringUtils.equals(this.target, tgtNs)) { return null; }
		} else {
			if (!StringUtils.equalsIgnoreCase(this.target, tgtNs)) { return null; }
		}

		final String baseName = m.group(2);
		for (String srcNs : this.sourceValues) {
			String srcAttName = String.format("%s:%s", srcNs, baseName);
			CmfAttribute<CmfValue> srcAtt = findMatchingAttribute(srcAttName, object);
			if (srcAtt != null) { return srcAtt; }
		}
		return null;
	}
}