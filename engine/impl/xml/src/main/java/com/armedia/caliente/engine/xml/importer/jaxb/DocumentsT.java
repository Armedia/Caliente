package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "documents")
@XmlType(name = "documents.t", propOrder = {
	"document"
})
public class DocumentsT extends AggregatorBase<DocumentT> {

	public DocumentsT() {
		super("document");
	}

	@XmlElement(name = "document")
	public List<DocumentT> getDocument() {
		return getItems();
	}
}