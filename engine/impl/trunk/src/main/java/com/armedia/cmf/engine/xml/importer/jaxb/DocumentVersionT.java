package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documentVersion.t", propOrder = {
	"lastAccessDate", "lastAccessor", "historyId", "version", "current", "antecedentId", "contentSize", "contentHash",
	"contentLocation"
})
public class DocumentVersionT extends SysObjectT {

	@XmlElement(name = "lastAccessDate", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar lastAccessDate;

	@XmlElement(name = "lastAccessor", required = true)
	protected String lastAccessor;

	@XmlElement(name = "historyId", required = true)
	protected String historyId;

	@XmlElement(name = "version", required = true)
	protected String version;

	@XmlElement(name = "current", required = false)
	protected boolean current;

	@XmlElement(name = "antecedentId", required = true)
	protected String antecedentId;

	@XmlElement(name = "contentSize", required = false)
	protected int contentSize;

	@XmlElement(name = "contentHash", required = false)
	protected byte[] contentHash;

	@XmlElement(name = "contentLocation", required = true)
	protected String contentLocation;

	public XMLGregorianCalendar getLastAccessDate() {
		return this.lastAccessDate;
	}

	public void setLastAccessDate(XMLGregorianCalendar value) {
		this.lastAccessDate = value;
	}

	public String getLastAccessor() {
		return this.lastAccessor;
	}

	public void setLastAccessor(String value) {
		this.lastAccessor = value;
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

	public String getAntecedentId() {
		return this.antecedentId;
	}

	public void setAntecedentId(String value) {
		this.antecedentId = value;
	}

	public int getContentSize() {
		return this.contentSize;
	}

	public void setContentSize(int value) {
		this.contentSize = value;
	}

	public byte[] getContentHash() {
		return this.contentHash;
	}

	public void setContentHash(byte[] value) {
		this.contentHash = (value);
	}

	public String getContentLocation() {
		return this.contentLocation;
	}

	public void setContentLocation(String value) {
		this.contentLocation = value;
	}

	@Override
	public String toString() {
		return String
			.format(
				"DocumentVersionT [id=%s, parentId=%s, name=%s, type=%s, sourcePath=%s, creationDate=%s, creator=%s, modificationDate=%s, modifier=%s, lastAccessDate=%s, lastAccessor=%s, acl=%s, attributes=%s, historyId=%s, version=%s, current=%s, antecedentId=%s, contentSize=%s, contentHash=%s, contentLocation=%s]",
				this.id, this.parentId, this.name, this.type, this.sourcePath, this.creationDate, this.creator,
				this.modificationDate, this.modifier, this.lastAccessDate, this.lastAccessor, this.acl,
				this.attributes, this.historyId, this.version, this.current, this.antecedentId, this.contentSize,
				Arrays.toString(this.contentHash), this.contentLocation);
	}
}