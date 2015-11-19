package com.armedia.cmf.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documentIndexEntry.t", propOrder = {
	"id", "historyId", "version", "current", "path", "name", "location"
})
public class DocumentIndexEntryT {

	@XmlElement(name = "id", required = true)
	protected String id;

	@XmlElement(name = "historyId", required = true)
	protected String historyId;

	@XmlElement(name = "version", required = true)
	protected String version;

	@XmlElement(name = "current", required = true)
	protected boolean current;

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

	public String getHistoryId() {
		return this.historyId;
	}

	public void setHistoryId(String value) {
		this.historyId = value;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String value) {
		this.version = value;
	}

	public boolean isCurrent() {
		return this.current;
	}

	public void setCurrent(boolean value) {
		this.current = value;
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
		return String.format(
			"DocumentIndexEntryT [id=%s, historyId=%s, version=%s, current=%s, path=%s, name=%s, location=%s]",
			this.id, this.historyId, this.version, this.current, this.path, this.name, this.location);
	}
}