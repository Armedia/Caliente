package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.ResidualsMode;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaMember;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class MappingRendererSet implements MappingRenderer {

	private final SchemaMember<?> type;
	private final ResidualsMode residualsMode;
	private final Character separator;
	private final List<MappingRenderer> renderers;

	/**
	 * @param type
	 */
	public MappingRendererSet(SchemaMember<?> type, Character separator, ResidualsMode residualsMode,
		List<MappingRenderer> renderers) {
		this.type = type;
		this.renderers = Tools.freezeList(renderers);
		this.residualsMode = residualsMode;
		this.separator = separator;
	}

	public final SchemaMember<?> getType() {
		return this.type;
	}

	public List<MappingRenderer> getRenderers() {
		return this.renderers;
	}

	public final ResidualsMode getResidualsMode() {
		return this.residualsMode;
	}

	public Character getSeparator() {
		return this.separator;
	}

	/**
	 * Render the mapped values, and return the attribute values that were rendered.
	 *
	 * @param object
	 * @return the set of target attributes that were rendered
	 */
	@Override
	public Collection<AttributeValue> render(CmfObject<CmfValue> object) {
		Collection<AttributeValue> ret = new ArrayList<>();

		Set<String> mappedTargetNames = new HashSet<>();
		Set<String> mappedSourceNames = new HashSet<>();
		for (MappingRenderer r : this.renderers) {
			if (r == null) {
				continue;
			}

			Collection<AttributeValue> values = r.render(object);
			if ((values == null) || values.isEmpty()) {
				continue;
			}

			for (AttributeValue value : values) {
				final String sourceName = value.getSourceName();
				final String targetName = value.getTargetName();
				final boolean override = value.isOverride();

				if (!StringUtils.isBlank(sourceName)) {
					mappedSourceNames.add(sourceName);
				}

				if ((this.type != null) && !this.type.getAttributeNames().contains(targetName)) {
					// This attribute doesn't exist, and we're not handling residualsMode, we skip
					// it
					continue;
				}

				if (mappedTargetNames.contains(targetName) && !override) {
					// If this attribute is already mapped, but isn't being overridden, we simply
					// skip it
					continue;
				}

				mappedTargetNames.add(targetName);
				ret.add(value);
			}
		}
		return ret;
	}
}