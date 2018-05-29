package com.armedia.caliente.engine.dynamic.xml.mapper;

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

	public Character getSeparator() {
		if (this.separator == null) { return null; }
		return this.separator.charAt(0);
	}

	public void setSeparator(Character value) {
		this.separator = (value == null ? null : value.toString());
	}

}