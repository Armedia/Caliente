package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "folders")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "folders.t", propOrder = {
	"folder"
})
public class FoldersT {

	@XmlElement(name = "folder")
	protected List<FolderT> folder;

	public List<FolderT> getFolders() {
		if (this.folder == null) {
			this.folder = new ArrayList<FolderT>();
		}
		return this.folder;
	}

	@Override
	public String toString() {
		return String.format("FoldersT [folder=%s]", this.folder);
	}
}