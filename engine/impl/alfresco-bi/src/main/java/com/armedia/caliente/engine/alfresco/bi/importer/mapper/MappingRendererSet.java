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
import org.apache.commons.lang3.tuple.Triple;

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
			Collection<Triple<String, String, String>> rendered = r.render(object);
			if (rendered == null) {
				rendered = Collections.emptyList();
			}
			for (Triple<String, String, String> triple : rendered) {
				final String sourceName = triple.getLeft();
				final String expectedTargetName = triple.getRight();
				final String renderedValue = triple.getMiddle();

				if (!StringUtils.isBlank(sourceName)) {
					mappedSourceNames.add(sourceName);
				}

				if (!this.type.getAttributeNames().contains(expectedTargetName) && !this.residuals) {
					// This attribute doesn't exist, and we're not handling residuals, we skip it
					continue;
				}

				if (StringUtils.isEmpty(renderedValue)) {
					// Ignore empty or null values
					continue;
				}

				if (m.containsKey(expectedTargetName) && !r.isOverride()) {
					// If this attribute is already mapped, but isn't being overridden, we simply
					// skip it
					continue;
				}

				m.put(expectedTargetName, renderedValue);
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