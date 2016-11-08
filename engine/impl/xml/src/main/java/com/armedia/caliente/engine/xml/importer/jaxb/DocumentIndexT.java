package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "documentIndex")
@XmlType(name = "documentIndex.t", propOrder = {
	"document"
})
public class DocumentIndexT extends AggregatorBase<DocumentIndexEntryT> {

	public DocumentIndexT() {
		super("document");
	}

	@XmlElement(name = "document")
	public List<DocumentIndexEntryT> getDocument() {
		return getItems();
	}
}