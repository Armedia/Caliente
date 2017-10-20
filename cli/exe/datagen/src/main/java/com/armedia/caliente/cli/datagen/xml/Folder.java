package com.armedia.caliente.cli.datagen.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "folder.t", propOrder = {
	"children"
})
public class Folder extends FsObject {

	@XmlElements({
		@XmlElement(name = "folder", required = false, type = Folder.class),
		@XmlElement(name = "document", required = false, type = Document.class),

	})
	protected List<FsObject> children;

	public List<FsObject> getChildren() {
		if (this.children == null) {
			this.children = new ArrayList<>();
		}
		return this.children;
	}
}