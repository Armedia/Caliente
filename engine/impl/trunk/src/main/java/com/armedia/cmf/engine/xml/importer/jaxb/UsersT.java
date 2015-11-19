package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "users")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "users.t", propOrder = {
	"user"
})
public class UsersT {

	@XmlElement(name = "user", required = false)
	protected List<UserT> user;

	public List<UserT> getUser() {
		if (this.user == null) {
			this.user = new ArrayList<UserT>();
		}
		return this.user;
	}

}
