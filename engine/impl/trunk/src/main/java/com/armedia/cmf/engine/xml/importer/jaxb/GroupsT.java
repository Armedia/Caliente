package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
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

	public List<GroupT> getGroup() {
		if (this.group == null) {
			this.group = new ArrayList<GroupT>();
		}
		return this.group;
	}
}