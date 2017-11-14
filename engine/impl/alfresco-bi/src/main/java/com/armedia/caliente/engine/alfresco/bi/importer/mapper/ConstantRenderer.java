package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

class ConstantRenderer extends AttributeRendererImpl {
	private final Collection<AttributeValue> value;

	public ConstantRenderer(Mapping m) {
		super(m);
		this.value = Collections
			.singleton(new AttributeValue(m.getTgt(), this.separator, m.isOverride(), new CmfValue(m.getValue())));
	}

	@Override
	public Collection<AttributeValue> render(CmfObject<CmfValue> object) {
		return this.value;
	}
}