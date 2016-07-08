package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "folderIndex")
@XmlType(name = "folderIndex.t", propOrder = {
	"folder"
})
public class FolderIndexT extends AggregatorBase<FolderIndexEntryT> {

	public FolderIndexT() {
		super("folder");
	}

	@XmlElement(name = "folder")
	public List<FolderIndexEntryT> getFolder() {
		return getItems();
	}
}