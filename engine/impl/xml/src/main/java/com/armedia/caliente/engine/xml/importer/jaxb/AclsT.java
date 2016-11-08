package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "acls")
@XmlType(name = "acls.t", propOrder = {
	"acl"
})
public class AclsT extends AggregatorBase<AclT> {

	public AclsT() {
		super("acl");
	}

	@XmlElement(name = "acl")
	public List<AclT> getAcl() {
		return getItems();
	}
}