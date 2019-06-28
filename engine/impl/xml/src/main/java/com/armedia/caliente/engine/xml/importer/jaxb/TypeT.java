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
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "type.t", propOrder = {
	"name", "superType", "attributes", "properties"
})
public class TypeT {

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "superType", required = false)
	protected String superType;

	@XmlElementWrapper(name = "attributes", required = false)
	@XmlElement(name = "attribute", required = false)
	protected List<AttributeDefT> attributes;

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

	public List<AttributeDefT> getAttributes() {
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

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getSuperType() {
		return this.superType;
	}

	public void setSuperType(String value) {
		this.superType = value;
	}

	@Override
	public String toString() {
		return String.format("TypeT [name=%s, superType=%s, attributes=%s]", this.name, this.superType,
			this.attributes);
	}
}