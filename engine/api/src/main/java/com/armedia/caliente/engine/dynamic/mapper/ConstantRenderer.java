package com.armedia.caliente.engine.dynamic.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.dynamic.xml.mapper.Mapping;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

class ConstantRenderer extends AttributeRenderer {

	private final Collection<AttributeMapping> value;

	public ConstantRenderer(Mapping m, Character parentSeparator) {
		super(m, parentSeparator);
		List<CmfValue> values = new ArrayList<>();
		for (String v : Tools.splitEscaped(this.separator, m.getValue())) {
			values.add(new CmfValue(v));
		}
		this.value = Collections.singleton(new AttributeMapping(m.getTgt(), this.separator, m.isOverride(), values));
	}

	@Override
	public Collection<AttributeMapping> render(CmfObject<CmfValue> object, ResidualsModeTracker tracker) {
		return this.value;
	}
}