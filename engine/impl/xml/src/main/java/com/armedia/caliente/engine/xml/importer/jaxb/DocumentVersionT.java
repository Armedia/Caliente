/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documentVersion.t", propOrder = {
	"format", "lastAccessDate", "lastAccessor", "historyId", "version", "current", "antecedentId", "contents"
})
public class DocumentVersionT extends SysObjectT {

	@XmlElement(name = "format", required = false)
	protected String format;

	@XmlElement(name = "lastAccessDate", required = false)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar lastAccessDate;

	@XmlElement(name = "lastAccessor", required = false)
	protected String lastAccessor;

	@XmlElement(name = "historyId", required = true)
	protected String historyId;

	@XmlElement(name = "version", required = true)
	protected String version;

	@XmlElement(name = "current", required = false)
	protected boolean current;

	@XmlElement(name = "antecedentId", required = true)
	protected String antecedentId;

	@XmlElementWrapper(name = "contents", required = false)
	@XmlElement(name = "content", required = false)
	protected List<ContentStreamT> contents;

	public String getFormat() {
		return this.format;
	}

	public void setFormat(String format) {
		this.format = format;
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

	public List<ContentStreamT> getContents() {
		if (this.contents == null) {
			this.contents = new ArrayList<>();
		}
		return this.contents;
	}

	@Override
	public String toString() {
		return String.format(
			"DocumentVersionT [id=%s, parentId=%s, name=%s, type=%s, sourcePath=%s, creationDate=%s, creator=%s, modificationDate=%s, modifier=%s, format=%s, lastAccessDate=%s, lastAccessor=%s, acl=%s, attributes=%s, historyId=%s, version=%s, current=%s, antecedentId=%s, contents=%s]",
			this.id, this.parentId, this.name, this.type, this.sourcePath, this.creationDate, this.creator,
			this.modificationDate, this.modifier, this.format, this.lastAccessDate, this.lastAccessor, this.acl,
			this.attributes, this.historyId, this.version, this.current, this.antecedentId, this.contents);
	}
}