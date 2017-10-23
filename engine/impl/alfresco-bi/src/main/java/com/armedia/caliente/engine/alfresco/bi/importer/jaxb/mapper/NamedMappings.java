package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "namedMappingSet.t")
public class NamedMappings extends MappingSet {

	@XmlAttribute(name = "includes", required = false)
	protected String includes;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	public String getIncludes() {
		return this.includes;
	}

	public void setIncludes(String value) {
		this.includes = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

}