package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

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

	private Set<String> getSet(Collection<String> c) {
		TreeSet<String> s = new TreeSet<>();
		if (c != null) {
			for (String str : c) {
				str = StringUtils.strip(str);
				if (str != null) {
					s.add(str);
				}
			}
		}
		return s;
	}

	private List<String> getList(List<String> l) {
		return (l != null ? l : new ArrayList<String>());
	}

	public List<String> getUser() {
		return (this.user = getList(this.user));
	}

	public Set<String> getUserSet() {
		return getSet(getUser());
	}

	public List<String> getGroup() {
		return (this.group = getList(this.group));
	}

	public Set<String> getGroupSet() {
		return getSet(getGroup());
	}

	public List<String> getRole() {
		return (this.role = getList(this.role));
	}

	public Set<String> getRoleSet() {
		return getSet(getRole());
	}
}