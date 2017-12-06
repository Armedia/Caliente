package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

class ConstantRenderer extends AttributeRenderer {

	private final Collection<AttributeValue> value;

	public ConstantRenderer(Mapping m, Character parentSeparator) {
		super(m, parentSeparator);
		List<CmfValue> values = new ArrayList<>();
		for (String v : Tools.splitEscaped(this.separator, m.getValue())) {
			values.add(new CmfValue(v));
		}
		this.value = Collections.singleton(new AttributeValue(m.getTgt(), this.separator, m.isOverride(), values));
	}

	@Override
	public Collection<AttributeValue> render(CmfObject<CmfValue> object, ResidualsModeTracker tracker) {
		return this.value;
	}
}