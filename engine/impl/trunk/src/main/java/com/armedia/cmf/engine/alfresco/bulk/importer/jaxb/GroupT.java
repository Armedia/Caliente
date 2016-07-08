package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "group.t", propOrder = {
	"name", "type", "email", "source", "administrator", "displayName", "users", "groups", "attributes", "properties"
})
public class GroupT implements Comparable<GroupT> {

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "type", required = true)
	protected String type;

	@XmlElement(name = "email", required = true)
	protected String email;

	@XmlElement(name = "source", required = true)
	protected String source;

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

	@XmlElementWrapper(name = "attributes", required = false)
	@XmlElement(name = "attribute", required = false)
	protected List<AttributeT> attributes;

	@XmlElementWrapper(name = "properties", required = false)
	@XmlElement(name = "property", required = false)
	protected List<PropertyT> properties;

	@XmlTransient
	protected Set<String> userSet = new TreeSet<String>();

	@XmlTransient
	protected Set<String> groupSet = new TreeSet<String>();

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
		if ((this.users == null) && !this.userSet.isEmpty()) {
			this.users = new ArrayList<String>();
		}
		if ((this.userSet != null) && !this.userSet.isEmpty()) {
			this.users.clear();
			this.users.addAll(this.userSet);
		}
		if ((this.groups == null) && !this.groupSet.isEmpty()) {
			this.groups = new ArrayList<String>();
		}
		if ((this.groupSet != null) && !this.groupSet.isEmpty()) {
			this.groups.clear();
			this.groups.addAll(this.groupSet);
		}
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		sortAttributes();
		if (this.userSet == null) {
			this.userSet = new TreeSet<String>();
		}
		this.userSet.clear();
		if (this.users != null) {
			this.userSet.addAll(this.users);
		}
		if (this.groupSet == null) {
			this.groupSet = new TreeSet<String>();
		}
		this.groupSet.clear();
		if (this.groups != null) {
			this.groupSet.addAll(this.groups);
		}
	}

	public List<PropertyT> getProperties() {
		if (this.properties == null) {
			this.properties = new ArrayList<PropertyT>();
		}
		return this.properties;
	}

	public List<AttributeT> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new ArrayList<AttributeT>();
		}
		return this.attributes;
	}

	public synchronized int getUserCount() {
		return this.userSet.size();
	}

	public synchronized boolean addUser(String user) {
		return this.userSet.add(user);
	}

	public synchronized boolean removeUser(String user) {
		return this.userSet.remove(user);
	}

	public synchronized boolean hasUser(String user) {
		return this.userSet.contains(user);
	}

	public synchronized void clearUsers() {
		this.userSet.clear();
	}

	public synchronized int getGroupCount() {
		return this.groupSet.size();
	}

	public synchronized boolean addGroup(String group) {
		return this.groupSet.add(group);
	}

	public synchronized boolean removeGroup(String group) {
		return this.groupSet.remove(group);
	}

	public synchronized boolean hasGroup(String group) {
		return this.groupSet.contains(group);
	}

	public synchronized void clearGroups() {
		this.groupSet.clear();
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

	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
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
	public int hashCode() {
		return Tools.hashTool(this, null, this.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		GroupT other = GroupT.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		return true;
	}

	@Override
	public int compareTo(GroupT o) {
		if (o == this) { return 0; }
		if (o == null) { return 1; }
		int r = Tools.compare(this.name, o.name);
		return r;
	}

	@Override
	public String toString() {
		return String.format(
			"GroupT [name=%s, type=%s, email=%s, administrator=%s, displayName=%s, users=%s, groups=%s]", this.name,
			this.type, this.email, this.administrator, this.displayName, this.users, this.groups);
	}
}