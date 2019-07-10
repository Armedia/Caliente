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

package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"description", "author", "published", "version", "imports", "namespaces", "dataTypes", "constraints", "types",
	"aspects"
})
@XmlRootElement(name = "model")
public class Model {

	@XmlAttribute(required = true)
	protected String name;

	@XmlElement
	protected String description;

	@XmlElement
	protected String author;

	@XmlSchemaType(name = "date")
	protected XMLGregorianCalendar published;

	@XmlElement
	protected String version;

	@XmlElementWrapper(name = "imports")
	@XmlElement(name = "import", required = true)
	protected List<Namespace> imports;

	@XmlElementWrapper(name = "namespaces")
	@XmlElement(name = "namespace", required = true)
	protected List<Namespace> namespaces;

	@XmlElementWrapper(name = "data-types")
	@XmlElement(name = "data-type", required = true)
	protected List<DataType> dataTypes;

	@XmlElementWrapper(name = "constraints")
	@XmlElement(name = "constraint", required = true)
	protected List<Constraint> constraints;

	@XmlElementWrapper(name = "types")
	@XmlElement(name = "type", required = true)
	protected List<ClassElement> types;

	@XmlElementWrapper(name = "aspects")
	@XmlElement(name = "aspect")
	protected List<ClassElement> aspects;

	/**
	 * Gets the value of the description property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the value of the description property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setDescription(String value) {
		this.description = value;
	}

	/**
	 * Gets the value of the author property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getAuthor() {
		return this.author;
	}

	/**
	 * Sets the value of the author property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setAuthor(String value) {
		this.author = value;
	}

	/**
	 * Gets the value of the published property.
	 *
	 * @return possible object is {@link XMLGregorianCalendar }
	 *
	 */
	public XMLGregorianCalendar getPublished() {
		return this.published;
	}

	/**
	 * Sets the value of the published property.
	 *
	 * @param value
	 *            allowed object is {@link XMLGregorianCalendar }
	 *
	 */
	public void setPublished(XMLGregorianCalendar value) {
		this.published = value;
	}

	/**
	 * Gets the value of the version property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Sets the value of the version property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setVersion(String value) {
		this.version = value;
	}

	/**
	 * Gets the value of the name property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the value of the name property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setName(String value) {
		this.name = value;
	}

	public List<Namespace> getImports() {
		return this.imports = ObjectFactory.getList(this.imports);
	}

	public List<Namespace> getNamespaces() {
		return this.namespaces = ObjectFactory.getList(this.namespaces);
	}

	public List<DataType> getDataTypes() {
		return this.dataTypes = ObjectFactory.getList(this.dataTypes);
	}

	public List<Constraint> getConstraints() {
		return this.constraints = ObjectFactory.getList(this.constraints);
	}

	public List<ClassElement> getTypes() {
		return this.types = ObjectFactory.getList(this.types);
	}

	public List<ClassElement> getAspects() {
		return this.aspects = ObjectFactory.getList(this.aspects);
	}
}