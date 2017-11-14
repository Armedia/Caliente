package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

class MappingRenderer {

	protected final String target;
	protected final Set<String> sourceValues;
	protected final char separator;
	protected final boolean caseSensitive;
	protected final boolean override;

	protected MappingRenderer(Mapping mapping) {
		this.target = StringUtils.strip(mapping.getTgt());
		this.caseSensitive = mapping.isCaseSensitive();
		this.separator = mapping.getSeparator();
		String value = StringUtils.strip(mapping.getValue());
		Set<String> sourceValues = new LinkedHashSet<>();
		for (String v : Tools.splitEscaped(this.separator, value)) {
			if (!this.caseSensitive) {
				v = StringUtils.upperCase(v);
			}
			sourceValues.add(v);
		}
		this.sourceValues = Tools.freezeSet(sourceValues);
		this.override = mapping.isOverride();
	}

	public final boolean isOverride() {
		return this.override;
	}

	protected static String renderValue(char separator, CmfAttribute<CmfValue> srcAtt) {
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

	/**
	 * <p>
	 * Render the mapped values, as a collection of {@link Triple} instances for which the
	 * {@link Triple#getLeft() left} value is the source attribute being mapped, the
	 * {@link Triple#getRight() right} value is the target attribute that the value should be mapped
	 * to, and the {@link Triple#getMiddle() middle} value is the actual rendered value (including
	 * separators, etc).
	 * </p>
	 * <p>
	 * As such, constant mappings have no source attribute, and namespace mappings may return
	 * multiple instances in the collection. An empty or {@code null} collection means nothing was
	 * rendered.
	 * </p>
	 *
	 *
	 * @param object
	 * @return the set of target attributes that were rendered
	 */
	public Collection<Triple<String, String, String>> render(CmfObject<CmfValue> object) {
		Collection<Triple<String, String, String>> ret = Collections.emptyList();
		if (this.caseSensitive) {
			// If the mapping is case sensitive, we don't need to do a scanning search
			for (final String sourceName : this.sourceValues) {
				if (!object.hasAttribute(sourceName)) {
					// No match!! Skip!
					continue;
				}

				// Match!!! Return it!!
				final String renderedValue = MappingRenderer.renderValue(this.separator,
					object.getAttribute(sourceName));
				ret = Collections.singletonList(Triple.of(sourceName, renderedValue, this.target));
				break;
			}
			return ret;
		}

		// The mapping is case-insensitive, so we actually need to do a scanning search

		for (final String sourceName : object.getAttributeNames()) {
			String sn = StringUtils.upperCase(sourceName);
			if (!this.sourceValues.contains(sn)) {
				// No match!! Skip!
				continue;
			}

			// Match!!! Return it!!
			final String renderedValue = MappingRenderer.renderValue(this.separator, object.getAttribute(sourceName));
			ret = Collections.singletonList(Triple.of(sourceName, renderedValue, this.target));
			break;
		}
		return ret;
	}

}