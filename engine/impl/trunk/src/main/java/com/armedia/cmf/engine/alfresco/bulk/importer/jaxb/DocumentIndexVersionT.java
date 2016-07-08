package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documentIndexVersion.t", propOrder = {
	"historyId", "version", "current", "format", "size"
})
public class DocumentIndexVersionT extends FolderIndexEntryT {

	@XmlElement(name = "historyId", required = true)
	protected String historyId;

	@XmlElement(name = "version", required = true)
	protected String version;

	@XmlElement(name = "current", required = true)
	protected boolean current;

	@XmlElement(name = "format", required = false)
	protected String format;

	@XmlElement(name = "size", required = true)
	protected long size;

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

	public String getFormat() {
		return this.format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public long getSize() {
		return this.size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@Override
	public String toString() {
		return String.format(
			"DocumentIndexEntryT [id=%s, path=%s, name=%s, location=%s, type=%s, historyId=%s, version=%s, current=%s, format=%s, size=%d]",
			this.id, this.path, this.name, this.location, this.type, this.historyId, this.version, this.current,
			this.format, this.size);
	}
}