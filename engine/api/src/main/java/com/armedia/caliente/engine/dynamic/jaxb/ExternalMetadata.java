package com.armedia.caliente.engine.dynamic.jaxb;

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

import com.armedia.caliente.engine.dynamic.jaxb.extmeta.MetadataSource;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"sources"
})
@XmlRootElement(name = "external-metadata")
public class ExternalMetadata extends XmlBase {

	@XmlElement(name = "source", required = false)
	protected List<MetadataSource> sources;

	public List<MetadataSource> getSources() {
		if (this.sources == null) {
			this.sources = new ArrayList<>();
		}
		return this.sources;
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