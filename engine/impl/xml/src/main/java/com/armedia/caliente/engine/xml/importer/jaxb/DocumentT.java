package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "document")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "document.t", propOrder = {
	"version"
})
public class DocumentT {

	@XmlElement(name = "version", required = true)
	protected List<DocumentVersionT> version;

	public List<DocumentVersionT> getVersion() {
		if (this.version == null) {
			this.version = new ArrayList<>();
		}
		return this.version;
	}

	@Override
	public String toString() {
		return String.format("DocumentT [version=%s]", this.version);
	}

}