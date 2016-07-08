package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "folders")
@XmlType(name = "folders.t", propOrder = {
	"folder"
})
public class FoldersT extends AggregatorBase<FolderT> {

	public FoldersT() {
		super("folder");
	}

	@XmlElement(name = "folder")
	public List<FolderT> getFolder() {
		return getItems();
	}
}