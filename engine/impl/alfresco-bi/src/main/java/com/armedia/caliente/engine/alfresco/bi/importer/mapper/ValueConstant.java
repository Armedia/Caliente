package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

class ValueConstant extends ValueMapping {
	private final String value;

	ValueConstant(Mapping m) {
		super(m);
		this.value = Tools.joinEscaped(this.separator, this.sourceValues);
	}

	@Override
	public String getValue(SchemaAttribute tgtAtt, CmfObject<CmfValue> object) {
		return (StringUtils.equals(tgtAtt.name, this.target) ? this.value : null);
	}
}