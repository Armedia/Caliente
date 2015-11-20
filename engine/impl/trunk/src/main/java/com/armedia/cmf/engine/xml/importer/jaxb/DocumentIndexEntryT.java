package com.armedia.cmf.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documentIndexEntry.t", propOrder = {
	"historyId", "version", "current",
})
public class DocumentIndexEntryT extends FolderIndexEntryT {

	@XmlElement(name = "historyId", required = true)
	protected String historyId;

	@XmlElement(name = "version", required = true)
	protected String version;

	@XmlElement(name = "current", required = true)
	protected boolean current;

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

	@Override
	public String toString() {
		return String.format(
			"DocumentIndexEntryT [id=%s, historyId=%s, version=%s, current=%s, path=%s, name=%s, location=%s]",
			this.id, this.historyId, this.version, this.current, this.path, this.name, this.location);
	}
}