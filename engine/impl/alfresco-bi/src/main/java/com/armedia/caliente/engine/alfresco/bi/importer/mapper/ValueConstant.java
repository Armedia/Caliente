package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.Properties;

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
	public MappedValue getValue(final SchemaAttribute tgtAtt, CmfObject<CmfValue> object) {
		return new MappedValue() {
			private final String name = tgtAtt.name;
			private final String value = ValueConstant.this.value;

			@Override
			public boolean render(Properties properties, CmfObject<CmfValue> object) {
				properties.setProperty(this.name, this.value);
				return true;
			}
		};
	}
}