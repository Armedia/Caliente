package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "documentIndex")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documentIndex.t", propOrder = {
	"document"
})
public class DocumentIndexT {

	@XmlElement(name = "document", required = false)
	protected List<DocumentIndexEntryT> document;

	public List<DocumentIndexEntryT> getDocument() {
		if (this.document == null) {
			this.document = new ArrayList<DocumentIndexEntryT>();
		}
		return this.document;
	}
}