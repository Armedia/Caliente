package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"source"
})
@XmlRootElement(name = "external-metadata")
public class ExternalMetadata {

	@XmlElement(name = "source", required = false)
	protected List<MetadataSourceDescriptor> source;

	public List<MetadataSourceDescriptor> getSource() {
		if (this.source == null) {
			this.source = new ArrayList<>();
		}
		return this.source;
	}

}