package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.xml.XmlBase;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeMappings.t", propOrder = {
	"commonMappings", "namedMappings"
})
@XmlRootElement(name = "attribute-mappings")
public class AttributeMappings extends XmlBase {

	@XmlTransient
	public static final String SCHEMA = "alfresco-bi.xsd";

	@XmlElement(name = "common-mappings")
	protected MappingSet commonMappings;

	@XmlElements({
		@XmlElement(name = "named-mappings", type = NamedMappings.class, required = false),
		@XmlElement(name = "type-mappings", type = TypeMappings.class, required = false),
	})
	protected List<NamedMappings> namedMappings;

	public AttributeMappings() {
		super(AttributeMappings.SCHEMA);
	}

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