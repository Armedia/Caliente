package com.armedia.cmf.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "folderIndexEntry.t", propOrder = {
	"id", "path", "name", "location"
})
public class FolderIndexEntryT {

	@XmlElement(name = "id", required = true)
	protected String id;

	@XmlElement(name = "path", required = true)
	protected String path;

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "location", required = true)
	protected String location;

	public String getId() {
		return this.id;
	}

	public void setId(String value) {
		this.id = value;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String value) {
		this.path = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return String.format("DocumentIndexEntryT [id=%s, path=%s, name=%s, location=%s]", this.id, this.path,
			this.name, this.location);
	}
}