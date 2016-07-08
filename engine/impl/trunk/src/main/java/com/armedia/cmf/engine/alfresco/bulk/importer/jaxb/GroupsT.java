package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "groups")
@XmlType(name = "groups.t", propOrder = {
	"group"
})
public class GroupsT extends AggregatorBase<GroupT> {

	public GroupsT() {
		super("group");
	}

	@XmlElement(name = "group")
	public List<GroupT> getGroup() {
		return getItems();
	}
}