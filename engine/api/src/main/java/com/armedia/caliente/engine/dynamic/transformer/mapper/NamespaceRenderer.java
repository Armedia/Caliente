package com.armedia.caliente.engine.dynamic.transformer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.xml.mapper.NamespaceMapping;

class NamespaceRenderer extends AttributeRenderer {
	private static final Pattern NSPARSER = Pattern.compile("^([^:]+):(.+)$");

	public NamespaceRenderer(NamespaceMapping m, Character parentSeparator) {
		super(m, parentSeparator);
	}

	@Override
	public Collection<AttributeMapping> apply(DynamicObject object, ResidualsModeTracker tracker) {
		Objects.requireNonNull(object, "Must provide a source object to map against");

		Collection<AttributeMapping> ret = new ArrayList<>();
		object.getAtt().forEach((sourceName, attribute) -> {
			// First, get the source attribute's namespace
			Matcher m = NamespaceRenderer.NSPARSER.matcher(sourceName);
			if (!m.matches()) {
				// No namespace!! Not applicable
				return;
			}

			// We have a namespace...so should it be mapped?
			String srcNs = m.group(1);
			if (!this.caseSensitive) {
				srcNs = StringUtils.upperCase(srcNs);
			}

			if (!this.sourceValues.contains(srcNs)) {
				// No match...skip it!
				return;
			}

			final String attributeBaseName = m.group(2);
			final String targetName = String.format("%s:%s", this.target, attributeBaseName);
			ret.add(new AttributeMapping(attribute, targetName, this.separator, this.override));
		});
		return ret;
	}
}