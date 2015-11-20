package com.armedia.cmf.engine.xml.importer.jaxb;

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

	public synchronized int getUserCount() {
		return this.user.size();
	}

	public synchronized boolean addUser(UserT user) {
		return this.user.add(user);
	}

	public synchronized boolean removeUser(UserT user) {
		return this.user.remove(user);
	}

	public synchronized boolean hasUser(UserT user) {
		return this.user.contains(user);
	}

	public synchronized void clearUsers() {
		this.user.clear();
	}

	@Override
	public String toString() {
		return String.format("UsersT [user=%s]", this.user);
	}
}