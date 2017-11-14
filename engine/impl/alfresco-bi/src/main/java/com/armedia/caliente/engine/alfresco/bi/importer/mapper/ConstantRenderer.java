package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.tuple.Triple;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

class ConstantRenderer extends MappingRenderer {
	private final Collection<Triple<String, String, String>> ret;

	public ConstantRenderer(Mapping m) {
		super(m);
		this.ret = Collections.singleton(Triple.of((String) null, m.getValue(), m.getTgt()));
	}

	@Override
	public Collection<Triple<String, String, String>> render(CmfObject<CmfValue> object) {
		return this.ret;
	}
}