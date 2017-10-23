package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeMappings.t", propOrder = {
	"commonMappings", "namedMappings"
})
public class AttributeMappings {

	@XmlElement(name = "common-mappings")
	protected MappingSet commonMappings;

	@XmlElements({
		@XmlElement(name = "mappings", type = NamedMappings.class, required = false),
		@XmlElement(name = "type-mappings", type = TypeMappings.class, required = false),
		@XmlElement(name = "secondary-mappings", type = SecondaryMappings.class, required = false)
	})
	protected List<NamedMappings> namedMappings;

	public MappingSet getCommonMappings() {
		return this.commonMappings;
	}

	public void setCommonMappings(MappingSet value) {
		this.commonMappings = value;
	}

	public List<NamedMappings> getMappings() {
		if (this.namedMappings == null) {
			this.namedMappings = new ArrayList<>();
		}
		return this.namedMappings;
	}
}