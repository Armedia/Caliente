package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "group.t", propOrder = {
	"name", "type", "email", "administrator", "displayName", "users", "groups"
})
public class GroupT {

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "type", required = true)
	protected String type;

	@XmlElement(name = "email", required = true)
	protected String email;

	@XmlElement(name = "administrator", required = true)
	protected String administrator;

	@XmlElement(name = "displayName", required = true)
	protected String displayName;

	@XmlElementWrapper(name = "users", required = false)
	@XmlElement(name = "user", required = false)
	protected List<String> users;

	@XmlElementWrapper(name = "groups", required = false)
	@XmlElement(name = "group", required = false)
	protected List<String> groups;

	public List<String> getUsers() {
		if (this.users == null) {
			this.users = new ArrayList<String>();
		}
		return this.users;
	}

	public List<String> getGroups() {
		if (this.groups == null) {
			this.groups = new ArrayList<String>();
		}
		return this.groups;
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

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String value) {
		this.email = value;
	}

	public String getAdministrator() {
		return this.administrator;
	}

	public void setAdministrator(String value) {
		this.administrator = value;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String value) {
		this.displayName = value;
	}

	@Override
	public String toString() {
		return String.format(
			"GroupT [name=%s, type=%s, email=%s, administrator=%s, displayName=%s, users=%s, groups=%s]", this.name,
			this.type, this.email, this.administrator, this.displayName, this.users, this.groups);
	}
}