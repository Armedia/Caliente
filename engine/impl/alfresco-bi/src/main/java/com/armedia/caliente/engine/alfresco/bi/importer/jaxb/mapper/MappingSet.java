package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mappingSet.t", propOrder = {
	"mappingElements", "residuals"
})
@XmlSeeAlso({
	NamedMappings.class
})
public class MappingSet {

	@XmlElements({
		@XmlElement(name = "map", type = NameMapping.class, required = false),
		@XmlElement(name = "set", type = SetValue.class, required = false),
		@XmlElement(name = "include", type = IncludeNamed.class, required = false),
		@XmlElement(name = "nsmap", type = NamespaceMapping.class, required = false)
	})
	protected List<MappingElement> mappingElements;

	@XmlElement(name = "residuals", required = false)
	@XmlJavaTypeAdapter(ResidualsModeAdapter.class)
	protected ResidualsMode residuals;

	@XmlAttribute(name = "separator")
	protected String separator;

	public List<MappingElement> getMappingElements() {
		if (this.mappingElements == null) {
			this.mappingElements = new ArrayList<>();
		}
		return this.mappingElements;
	}

	public void setResidualsMode(ResidualsMode mode) {
		this.residuals = mode;
	}

	public ResidualsMode getResidualsMode() {
		return this.residuals;
	}

	public String getSeparator() {
		return Tools.coalesce(this.separator, ",");
	}

	public void setSeparator(String value) {
		this.separator = value;
	}

}