/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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
	"id", "parentId", "name", "type", "sourcePath", "creationDate", "creator", "modificationDate", "modifier", "acl",
	"attributes", "properties"
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

	@XmlElement(name = "acl", required = true)
	protected String acl;

	@XmlElementWrapper(name = "attributes", required = false)
	@XmlElement(name = "attribute", required = false)
	protected List<AttributeT> attributes;

	@XmlElementWrapper(name = "properties", required = false)
	@XmlElement(name = "property", required = false)
	protected List<PropertyT> properties;

	protected void sortAttributes() {
		if (this.attributes != null) {
			Collections.sort(this.attributes);
		}
		if (this.properties != null) {
			Collections.sort(this.properties);
		}
	}

	protected void beforeMarshal(Marshaller m) {
		sortAttributes();
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		sortAttributes();
	}

	public List<AttributeT> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new ArrayList<>();
		}
		return this.attributes;
	}

	public List<PropertyT> getProperties() {
		if (this.properties == null) {
			this.properties = new ArrayList<>();
		}
		return this.properties;
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

	public String getAcl() {
		return this.acl;
	}

	public void setAcl(String value) {
		this.acl = value;
	}

	@Override
	public String toString() {
		return String.format(
			"SysObjectT [id=%s, parentId=%s, name=%s, type=%s, sourcePath=%s, creationDate=%s, creator=%s, modificationDate=%s, modifier=%s, acl=%s, attributes=%s, properties=%s]",
			this.id, this.parentId, this.name, this.type, this.sourcePath, this.creationDate, this.creator,
			this.modificationDate, this.modifier, this.acl, this.attributes, this.properties);
	}
}