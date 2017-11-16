package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "principalMappings.t", propOrder = {
	"user", "group", "role"
})
@XmlRootElement(name = "principal-mappings")
public class PrincipalMappings {

	@XmlElementWrapper(name = "user")
	@XmlElement(name = "attribute")
	protected List<String> user;

	@XmlElementWrapper(name = "group")
	@XmlElement(name = "attribute")
	protected List<String> group;

	@XmlElementWrapper(name = "role")
	@XmlElement(name = "attribute")
	protected List<String> role;

	public List<String> getUser() {
		if (this.user == null) {
			this.user = new ArrayList<>();
		}
		return this.user;
	}

	public List<String> getGroup() {
		if (this.group == null) {
			this.group = new ArrayList<>();
		}
		return this.group;
	}

	public List<String> getRole() {
		if (this.role == null) {
			this.role = new ArrayList<>();
		}
		return this.role;
	}
}