package com.armedia.cmf.engine.xml.importer.jaxb;

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

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "user.t", propOrder = {
	"name", "defaultFolder", "description", "email", "loginName", "loginDomain", "osName", "osDomain", "attributes",
	"properties"
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
			this.attributes = new ArrayList<AttributeT>();
		}
		return this.attributes;
	}

	public List<PropertyT> getProperties() {
		if (this.properties == null) {
			this.properties = new ArrayList<PropertyT>();
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
		if (!Tools.equals(this.name, other.name)) { return false; }
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