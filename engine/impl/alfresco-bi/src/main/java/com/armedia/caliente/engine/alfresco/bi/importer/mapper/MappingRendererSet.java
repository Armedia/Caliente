package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class MappingRendererSet {

	private final AlfrescoType type;
	private final boolean residuals;
	private final List<MappingRenderer> renderers;

	/**
	 * @param type
	 */
	public MappingRendererSet(AlfrescoType type, boolean residuals, List<MappingRenderer> renderers) {
		this.type = type;
		this.renderers = Tools.freezeList(renderers);
		this.residuals = residuals;
	}

	public final AlfrescoType getType() {
		return this.type;
	}

	public final String getSignature() {
		return this.type.getSignature();
	}

	private String renderValue(char separator, Collection<CmfValue> values) {
		if (values == null) { return null; }
		if (values.isEmpty()) { return StringUtils.EMPTY; }
		Collection<String> rendered = new ArrayList<>(values.size());
		for (CmfValue value : values) {
			// Avoid null values
			if (value.isNull()) {
				continue;
			}
			rendered.add(value.asString());
		}
		return Tools.joinEscaped(separator, rendered);
	}

	/**
	 * Render the mapped values, and return the attribute values that were rendered.
	 *
	 * @param object
	 * @return the set of target attributes that were rendered
	 */
	public Map<String, String> render(CmfObject<CmfValue> object) {
		Map<String, String> m = new TreeMap<>();

		Set<String> mappedSourceNames = new HashSet<>();
		for (MappingRenderer r : this.renderers) {
			if (r == null) {
				continue;
			}
			Collection<AttributeValue> values = r.render(object);
			if (values == null) {
				values = Collections.emptyList();
			}
			for (AttributeValue value : values) {
				final String sourceName = value.getSourceName();
				final String targetName = value.getTargetName();
				final String renderedValue = renderValue(value.getSeparator(), value.getValues());
				final boolean override = value.isOverride();

				if (!StringUtils.isBlank(sourceName)) {
					mappedSourceNames.add(sourceName);
				}

				if (!this.type.getAttributeNames().contains(targetName) && !this.residuals) {
					// This attribute doesn't exist, and we're not handling residuals, we skip it
					continue;
				}

				if (StringUtils.isEmpty(renderedValue)) {
					// Ignore empty or null values
					continue;
				}

				if (m.containsKey(targetName) && !override) {
					// If this attribute is already mapped, but isn't being overridden, we simply
					// skip it
					continue;
				}

				m.put(targetName, renderedValue);
			}
		}

		if (this.residuals) {
			for (String sourceName : object.getAttributeNames()) {
				if (!mappedSourceNames.contains(sourceName)) {
					// Add this residual
					CmfAttribute<CmfValue> attribute = object.getAttribute(sourceName);
					String v = renderAttribute(attribute, ',');
					m.put(sourceName, v);
				}
			}
		}

		return m;
	}

	private String renderAttribute(CmfAttribute<CmfValue> attribute, char sep) {
		List<String> values = new ArrayList<>(attribute.getValueCount());
		for (CmfValue v : attribute) {
			values.add(v.asString());
		}
		return Tools.joinEscaped(sep, values);
	}

}