package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.AttributeMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.MappingSet;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.NamedMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.NamespaceMapping;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.SetValue;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class AttributeMapper {

	public AttributeMapper() throws Exception {
		AttributeMappings xml = null;
		MappingSet commonMappings = xml.getCommonMappings();
		Collection<ValueMapping> common = new ArrayList<>();
		if (commonMappings != null) {
			for (Mapping m : commonMappings.getMappings()) {
				ValueMapping M = null;
				if (SetValue.class.isInstance(m)) {
					M = new ValueConstant(m);
				} else if (NamespaceMapping.class.isInstance(m)) {
					M = new ValueMappingByNamespace(m);
				} else {
					M = new ValueMapping(m);
				}
				common.add(M);
			}
		}

		Map<String, Collection<ValueMapping>> named = new TreeMap<>();
		List<NamedMappings> namedMappings = xml.getMappings();
		if (namedMappings != null) {
			for (NamedMappings mappingSet : namedMappings) {
				if (named.containsKey(mappingSet.getName())) {
					// ERROR! Duplicate name
					throw new Exception(String.format("Duplicate mapping set name [%s]", mappingSet.getName()));
				}
			}
		}
	}

	public String getMappedValue(SchemaAttribute tgtAtt, CmfObject<CmfValue> object) {
		return null;
	}

}
