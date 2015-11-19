package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "acl.t", propOrder = {
	"id", "description", "users", "groups"
})
public class AclT {

	@XmlElement(name = "id", required = true)
	protected String id;

	@XmlElement(name = "description", required = true)
	protected String description;

	@XmlElement(name = "users", required = false)
	protected List<AclPermitT> users;

	@XmlElement(name = "groups", required = false)
	protected List<AclPermitT> groups;

	public List<AclPermitT> getUsers() {
		if (this.users == null) {
			this.users = new ArrayList<AclPermitT>();
		}
		return this.users;
	}

	public List<AclPermitT> getGroups() {
		if (this.groups == null) {
			this.groups = new ArrayList<AclPermitT>();
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
}