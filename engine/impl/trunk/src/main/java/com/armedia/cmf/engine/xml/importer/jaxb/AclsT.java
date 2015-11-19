package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "acls")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "acls.t", propOrder = {
	"acl"
})
public class AclsT {

	@XmlElement(name = "acl", required = false)
	protected List<AclT> acl;

	public List<AclT> getAcl() {
		if (this.acl == null) {
			this.acl = new ArrayList<AclT>();
		}
		return this.acl;
	}
}