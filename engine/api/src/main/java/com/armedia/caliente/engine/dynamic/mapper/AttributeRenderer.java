package com.armedia.caliente.engine.dynamic.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.xml.mapper.Mapping;
import com.armedia.commons.utilities.Tools;

class AttributeRenderer implements MappingRenderer {

	private static final char DEFAULT_SEPARATOR = ',';

	protected final String target;
	protected final Set<String> sourceValues;
	protected final char separator;
	protected final boolean caseSensitive;
	protected final boolean override;

	protected AttributeRenderer(Mapping mapping, Character parentSeparator) {
		this.target = StringUtils.strip(mapping.getTgt());
		this.caseSensitive = mapping.isCaseSensitive();
		this.separator = Tools.coalesce(mapping.getSeparator(), parentSeparator, AttributeRenderer.DEFAULT_SEPARATOR);
		String value = StringUtils.strip(mapping.getValue());
		Set<String> sourceValues = new LinkedHashSet<>();
		Tools.splitEscaped(this.separator, value).forEach((v) -> {
			if (!this.caseSensitive) {
				v = StringUtils.upperCase(v);
			}
			sourceValues.add(v);
		});
		this.sourceValues = Tools.freezeSet(sourceValues);
		this.override = mapping.isOverride();
	}

	@Override
	public Collection<AttributeMapping> render(DynamicObject object, ResidualsModeTracker tracker) {
		if (this.caseSensitive) {
			// If the mapping is case sensitive, we don't need to do a scanning search
			for (String sourceName : this.sourceValues) {
				if (!object.getAtt().containsKey(sourceName)) {
					// No match!! Skip!
					continue;
				}

				// Match!!! Return it!!
				return Collections.singletonList(
					new AttributeMapping(object.getAtt().get(sourceName), this.target, this.separator, this.override));
			}
			return Collections.emptyList();
		}

		// The mapping is case-insensitive, so we actually need to do a scanning search
		for (final String sourceName : object.getAtt().keySet()) {
			String sn = StringUtils.upperCase(sourceName);
			if (!this.sourceValues.contains(sn)) {
				// No match!! Skip!
				continue;
			}

			// Match!!! Return it!!
			return Collections.singletonList(
				new AttributeMapping(object.getAtt().get(sourceName), this.target, this.separator, this.override));
		}

		// No match!! Return an empty list!
		return Collections.emptyList();
	}

}