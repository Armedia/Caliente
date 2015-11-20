package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "documents")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documents.t", propOrder = {
	"document"
})
public class DocumentsT {

	@XmlElement(name = "document")
	protected List<DocumentT> document;

	public List<DocumentT> getdocuments() {
		if (this.document == null) {
			this.document = new ArrayList<DocumentT>();
		}
		return this.document;
	}

	@Override
	public String toString() {
		return String.format("DocumentsT [document=%s]", this.document);
	}
}