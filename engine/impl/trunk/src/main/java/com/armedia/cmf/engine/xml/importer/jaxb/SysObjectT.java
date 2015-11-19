package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sysObject.t", propOrder = {
	"id", "parentId", "name", "type", "sourcePath", "creationDate", "creator", "modificationDate", "modifier",
	"lastAccessDate", "lastAccessor", "acl", "attributes"
})
@XmlSeeAlso({
	FolderT.class, DocumentVersionT.class
})
public class SysObjectT {

	@XmlElement(name = "id", required = true)
	protected String id;

	@XmlElement(name = "parentId", required = true)
	protected String parentId;

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "type", required = true)
	protected String type;

	@XmlElement(name = "sourcePath", required = true)
	protected String sourcePath;

	@XmlElement(name = "creationDate", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar creationDate;

	@XmlElement(name = "creator", required = true)
	protected String creator;

	@XmlElement(name = "modificationDate", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar modificationDate;

	@XmlElement(name = "modifier", required = true)
	protected String modifier;

	@XmlElement(name = "lastAccessDate", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar lastAccessDate;

	@XmlElement(name = "lastAccessor", required = true)
	protected String lastAccessor;

	@XmlElement(name = "acl", required = true)
	protected String acl;

	@XmlElementWrapper(name = "attributes", required = false)
	protected List<AttributeT> attributes;

	public List<AttributeT> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new ArrayList<AttributeT>();
		}
		return this.attributes;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String value) {
		this.id = value;
	}

	public String getParentId() {
		return this.parentId;
	}

	public void setParentId(String value) {
		this.parentId = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String value) {
		this.type = value;
	}

	public String getSourcePath() {
		return this.sourcePath;
	}

	public void setSourcePath(String value) {
		this.sourcePath = value;
	}

	public XMLGregorianCalendar getCreationDate() {
		return this.creationDate;
	}

	public void setCreationDate(XMLGregorianCalendar value) {
		this.creationDate = value;
	}

	public String getCreator() {
		return this.creator;
	}

	public void setCreator(String value) {
		this.creator = value;
	}

	public XMLGregorianCalendar getModificationDate() {
		return this.modificationDate;
	}

	public void setModificationDate(XMLGregorianCalendar value) {
		this.modificationDate = value;
	}

	public String getModifier() {
		return this.modifier;
	}

	public void setModifier(String value) {
		this.modifier = value;
	}

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

	public String getAcl() {
		return this.acl;
	}

	public void setAcl(String value) {
		this.acl = value;
	}

	@Override
	public String toString() {
		return String
			.format(
				"SysObjectT [id=%s, parentId=%s, name=%s, type=%s, sourcePath=%s, creationDate=%s, creator=%s, modificationDate=%s, modifier=%s, lastAccessDate=%s, lastAccessor=%s, acl=%s, attributes=%s]",
				this.id, this.parentId, this.name, this.type, this.sourcePath, this.creationDate, this.creator,
				this.modificationDate, this.modifier, this.lastAccessDate, this.lastAccessor, this.acl, this.attributes);
	}
}