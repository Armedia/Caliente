package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"source"
})
@XmlRootElement(name = "external-metadata")
public class ExternalMetadata {

	protected List<MetadataSource> source;

	public List<MetadataSource> getSource() {
		if (this.source == null) {
			this.source = new ArrayList<>();
		}
		return this.source;
	}

}