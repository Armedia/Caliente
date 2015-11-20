package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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

	protected void sortAttributes() {
		if (this.user != null) {
			Collections.sort(this.user);
		}
	}

	protected void beforeMarshal(Marshaller m) {
		sortAttributes();
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		sortAttributes();
	}

	public List<UserT> getUsers() {
		if (this.user == null) {
			this.user = new ArrayList<UserT>();
		}
		return this.user;
	}

	@Override
	public String toString() {
		return String.format("UsersT [user=%s]", this.user);
	}
}