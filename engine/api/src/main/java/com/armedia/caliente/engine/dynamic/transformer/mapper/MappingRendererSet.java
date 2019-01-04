package com.armedia.caliente.engine.dynamic.transformer.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.xml.mapper.ResidualsMode;
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
	public final Collection<AttributeMapping> render(DynamicObject object, ResidualsModeTracker tracker) {
		Map<String, AttributeMapping> ret = new TreeMap<>();
		if (tracker != null) {
			tracker.applyResidualsMode(this.residualsMode);
		}
		for (MappingRenderer r : this.renderers) {
			if (r == null) {
				continue;
			}

			Collection<AttributeMapping> values = r.render(object, tracker);
			if ((values == null) || values.isEmpty()) {
				continue;
			}

			for (AttributeMapping value : values) {
				final String targetName = value.getTargetName();
				if (value.isOverride() || !ret.containsKey(targetName)) {
					ret.put(targetName, value);
				}
			}
		}
		return ret.values();
	}
}