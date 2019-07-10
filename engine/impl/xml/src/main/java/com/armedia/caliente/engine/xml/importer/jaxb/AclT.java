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
@XmlType(name = "acl.t", propOrder = {
	"id", "description", "users", "groups", "attributes", "properties"
})
public class AclT {

	@XmlElement(name = "id", required = true)
	protected String id;

	@XmlElement(name = "description", required = true)
	protected String description;

	@XmlElementWrapper(name = "users", required = false)
	@XmlElement(name = "permit", required = false)
	protected List<AclPermitT> users;

	@XmlElementWrapper(name = "groups", required = false)
	@XmlElement(name = "permit", required = false)
	protected List<AclPermitT> groups;

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

	public List<AclPermitT> getUsers() {
		if (this.users == null) {
			this.users = new ArrayList<>();
		}
		return this.users;
	}

	public List<AclPermitT> getGroups() {
		if (this.groups == null) {
			this.groups = new ArrayList<>();
		}
		return this.groups;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String value) {
		this.id = value;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String value) {
		this.description = value;
	}

	@Override
	public String toString() {
		return String.format("AclT [id=%s, description=%s, users=%s, groups=%s]", this.id, this.description, this.users,
			this.groups);
	}
}