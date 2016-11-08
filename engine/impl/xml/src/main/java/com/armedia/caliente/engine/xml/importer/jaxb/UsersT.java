package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "users")
@XmlType(name = "users.t", propOrder = {
	"user"
})
public class UsersT extends AggregatorBase<UserT> {

	public UsersT() {
		super("user");
	}

	@XmlElement(name = "user")
	public List<UserT> getUser() {
		return getItems();
	}
}