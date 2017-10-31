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
	"mappings", "residuals"
})
@XmlSeeAlso({
	NamedMappings.class
})
public class MappingSet {

	@XmlElements({
		@XmlElement(name = "map", type = NameMapping.class, required = false),
		@XmlElement(name = "nsmap", type = NamespaceMapping.class, required = false)
	})
	protected List<Mapping> mappings;

	@XmlElement(name = "residuals", required = false)
	@XmlJavaTypeAdapter(ResidualsModeAdapter.class)
	protected ResidualsMode residuals;

	@XmlAttribute(name = "separator")
	protected String separator;

	public List<Mapping> getMappings() {
		if (this.mappings == null) {
			this.mappings = new ArrayList<>();
		}
		return this.mappings;
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