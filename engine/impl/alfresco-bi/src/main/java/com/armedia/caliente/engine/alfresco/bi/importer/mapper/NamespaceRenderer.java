package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class NamespaceRenderer extends MappingRenderer {
	private static final Pattern NSPARSER = Pattern.compile("^([^:]+):(.+)$");

	public NamespaceRenderer(Mapping m) {
		super(m);
	}

	@Override
	public Collection<Triple<String, String, String>> render(CmfObject<CmfValue> object) {
		Objects.requireNonNull(object, "Must provide a source object to map against");

		Collection<Triple<String, String, String>> ret = new ArrayList<>();
		for (final String sourceName : object.getAttributeNames()) {
			// First, get the source attribute's namespace
			Matcher m = NamespaceRenderer.NSPARSER.matcher(sourceName);
			if (!m.matches()) {
				// No namespace!! Not applicable
				continue;
			}

			// We have a namespace...so should it be mapped?
			String srcNs = m.group(1);
			if (!this.caseSensitive) {
				srcNs = StringUtils.upperCase(srcNs);
			}

			if (!this.sourceValues.contains(srcNs)) {
				// No match...skip it!
				continue;
			}

			final String attributeBaseName = m.group(2);
			final String targetName = String.format("%s:%s", this.target, attributeBaseName);
			final String renderedValue = MappingRenderer.renderValue(this.separator, object.getAttribute(sourceName));
			ret.add(Triple.of(sourceName, renderedValue, targetName));
		}
		return ret;
	}
}