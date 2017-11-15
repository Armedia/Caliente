package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.ResidualsMode;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class MappingRendererSet implements MappingRenderer {

	private final String name;
	private final ResidualsMode residualsMode;
	private final Character separator;
	private final List<MappingRenderer> renderers;

	public MappingRendererSet(String name, Character separator, ResidualsMode residualsMode,
		List<MappingRenderer> renderers) {
		this.name = name;
		this.renderers = Tools.freezeList(renderers);
		this.residualsMode = residualsMode;
		this.separator = separator;
	}

	public String getName() {
		return this.name;
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
	public final Collection<AttributeValue> render(CmfObject<CmfValue> object) {
		Map<String, AttributeValue> ret = new TreeMap<>();

		for (MappingRenderer r : this.renderers) {
			if (r == null) {
				continue;
			}

			Collection<AttributeValue> values = r.render(object);
			if ((values == null) || values.isEmpty()) {
				continue;
			}

			for (AttributeValue value : values) {
				final String targetName = value.getTargetName();
				if (value.isOverride() || !ret.containsKey(targetName)) {
					ret.put(targetName, value);
				}
			}
		}
		return ret.values();
	}
}