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