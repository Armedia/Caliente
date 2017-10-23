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
	"mappings", "residualMarker"
})
@XmlSeeAlso({
	NamedMappings.class
})
public class MappingSet {

	private static final ResidualsMarker RESIDUALS_ENABLE = new ResidualsEnable();
	private static final ResidualsMarker RESIDUALS_DISABLE = new ResidualsDisable();

	@XmlElements({
		@XmlElement(name = "map", type = Mapping.class, required = false),
		@XmlElement(name = "nsmap", type = NamespaceMapping.class, required = false)
	})
	protected List<Mapping> mappings;

	@XmlElements({
		@XmlElement(name = "enable-residuals", type = ResidualsEnable.class, required = false),
		@XmlElement(name = "disable-residuals", type = ResidualsDisable.class, required = false),
	})
	protected ResidualsMarker residualMarker;

	@XmlAttribute(name = "separator")
	protected String separator;

	public List<Mapping> getMappings() {
		if (this.mappings == null) {
			this.mappings = new ArrayList<>();
		}
		return this.mappings;
	}

	public void setEnableResiduals(Boolean value) {
		if (value == null) {
			this.residualMarker = null;
		} else {
			this.residualMarker = (value ? MappingSet.RESIDUALS_ENABLE : MappingSet.RESIDUALS_DISABLE);
		}
	}

	public Boolean getEnableResiduals() {
		if (this.residualMarker == null) { return null; }
		return this.residualMarker.isResidualsEnabled();
	}

	public String getSeparator() {
		return Tools.coalesce(this.separator, ",");
	}

	public void setSeparator(String value) {
		this.separator = value;
	}

}