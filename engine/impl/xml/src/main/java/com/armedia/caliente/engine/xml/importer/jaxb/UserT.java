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
import java.util.Objects;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "user.t", propOrder = {
	"name", "defaultFolder", "description", "email", "source", "loginName", "loginDomain", "osName", "osDomain",
	"attributes", "properties"
})
public class UserT implements Comparable<UserT> {

	@XmlElement(required = true)
	protected String name;

	@XmlElement(required = true)
	protected String defaultFolder;

	@XmlElement(required = true)
	protected String description;

	@XmlElement(required = true)
	protected String email;

	@XmlElement(required = true)
	protected String source;

	@XmlElement(required = true)
	protected String loginName;

	@XmlElement(required = true)
	protected String loginDomain;

	@XmlElement(required = true)
	protected String osName;

	@XmlElement(required = true)
	protected String osDomain;

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

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getDefaultFolder() {
		return this.defaultFolder;
	}

	public void setDefaultFolder(String value) {
		this.defaultFolder = value;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String value) {
		this.description = value;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String value) {
		this.email = value;
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getLoginName() {
		return this.loginName;
	}

	public void setLoginName(String value) {
		this.loginName = value;
	}

	public String getLoginDomain() {
		return this.loginDomain;
	}

	public void setLoginDomain(String value) {
		this.loginDomain = value;
	}

	public String getOsName() {
		return this.osName;
	}

	public void setOsName(String value) {
		this.osName = value;
	}

	public String getOsDomain() {
		return this.osDomain;
	}

	public void setOsDomain(String value) {
		this.osDomain = value;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		UserT other = UserT.class.cast(obj);
		if (!Objects.equals(this.name, other.name)) { return false; }
		return true;
	}

	@Override
	public int compareTo(UserT o) {
		if (o == this) { return 0; }
		if (o == null) { return 1; }
		int r = Tools.compare(this.name, o.name);
		return r;
	}

	@Override
	public String toString() {
		return String.format(
			"UserT [name=%s, defaultFolder=%s, description=%s, email=%s, loginName=%s, loginDomain=%s, osName=%s, osDomain=%s]",
			this.name, this.defaultFolder, this.description, this.email, this.loginName, this.loginDomain, this.osName,
			this.osDomain);
	}
}