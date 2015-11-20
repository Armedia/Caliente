package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "groups")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "groups.t", propOrder = {
	"group"
})
public class GroupsT {

	@XmlElement(name = "group")
	protected List<GroupT> group;

	public synchronized int getGroupCount() {
		return this.group.size();
	}

	public synchronized boolean addGroup(GroupT group) {
		return this.group.add(group);
	}

	public synchronized boolean removeGroup(GroupT group) {
		return this.group.remove(group);
	}

	public synchronized boolean hasGroup(GroupT group) {
		return this.group.contains(group);
	}

	public synchronized void clearGroups() {
		this.group.clear();
	}

	@Override
	public String toString() {
		return String.format("GroupsT [group=%s]", this.group);
	}
}