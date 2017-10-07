package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataTransformNames.t", propOrder = {
	"map", "defaultTransform"
})
public class TransformAttributeNames {

	@XmlElement(name = "map", required = false)
	protected List<MetadataNameMapping> map;

	@XmlElement(name = "default", required = false)
	protected String defaultTransform;

	public List<MetadataNameMapping> getMap() {
		if (this.map == null) {
			this.map = new ArrayList<>();
		}
		return this.map;
	}

	public String getDefaultTransform() {
		return this.defaultTransform;
	}

	public void setDefaultTransform(String value) {
		this.defaultTransform = value;
	}

}