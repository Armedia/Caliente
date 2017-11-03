package com.armedia.caliente.engine.dynamic.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamReader;

import com.armedia.caliente.engine.dynamic.xml.metadata.MetadataSet;
import com.armedia.caliente.engine.dynamic.xml.metadata.MetadataSource;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"metadataSources", "metadataSets"
})
@XmlRootElement(name = "external-metadata")
public class ExternalMetadata extends XmlBase {

	@XmlElement(name = "data-source", required = false)
	protected List<MetadataSource> metadataSources;

	@XmlElement(name = "metadata-set", required = false)
	protected List<MetadataSet> metadataSets;

	public List<MetadataSource> getMetadataSources() {
		if (this.metadataSources == null) {
			this.metadataSources = new ArrayList<>();
		}
		return this.metadataSources;
	}

	public List<MetadataSet> getMetadataSets() {
		if (this.metadataSets == null) {
			this.metadataSets = new ArrayList<>();
		}
		return this.metadataSets;
	}

	public static ExternalMetadata loadFromXML(InputStream in) throws JAXBException {
		return XmlBase.loadFromXML(ExternalMetadata.class, in);
	}

	public static ExternalMetadata loadFromXML(Reader in) throws JAXBException {
		return XmlBase.loadFromXML(ExternalMetadata.class, in);
	}

	public static ExternalMetadata loadFromXML(XMLStreamReader in) throws JAXBException {
		return XmlBase.loadFromXML(ExternalMetadata.class, in);
	}
}