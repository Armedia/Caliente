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

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mappingSet.t", propOrder = {
	"mappings", "enableResiduals", "disableResiduals"
})
@XmlSeeAlso({
	NamedMappings.class
})
public class MappingSet {

	private static final EmptyElement ELEMENT = new EmptyElement();

	@XmlElements({
		@XmlElement(name = "map", type = Mapping.class, required = false),
		@XmlElement(name = "nsmap", type = NamespaceMapping.class, required = false)
	})
	protected List<Mapping> mappings;

	@XmlElement(name = "enable-residuals")
	protected EmptyElement enableResiduals;

	@XmlElement(name = "disable-residuals")
	protected EmptyElement disableResiduals;

	@XmlAttribute(name = "separator")
	protected String separator;

	public List<Mapping> getMappings() {
		if (this.mappings == null) {
			this.mappings = new ArrayList<>();
		}
		return this.mappings;
	}

	public void setEnableResiduals(Boolean value) {
		this.enableResiduals = null;
		this.disableResiduals = null;
		if (value != null) {
			if (value) {
				this.enableResiduals = MappingSet.ELEMENT;
				this.disableResiduals = null;
			} else {
				this.enableResiduals = null;
				this.disableResiduals = MappingSet.ELEMENT;
			}
		}
	}

	public Boolean isEnableResiduals() {
		if ((this.enableResiduals != null) && (this.disableResiduals != null)) { throw new IllegalStateException(
			"May not have both enable and disable residuals"); }
		if (this.enableResiduals != null) { return Boolean.TRUE; }
		if (this.disableResiduals != null) { return Boolean.FALSE; }
		return null;
	}

	public String getSeparator() {
		return Tools.coalesce(this.separator, ",");
	}

	public void setSeparator(String value) {
		this.separator = value;
	}

}