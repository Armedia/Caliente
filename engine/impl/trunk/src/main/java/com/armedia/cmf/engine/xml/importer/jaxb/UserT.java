package com.armedia.cmf.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "user.t", propOrder = {
	"name", "defaultFolder", "description", "email", "loginName", "loginDomain", "osName", "osDomain", "defaultAcl"
})
public class UserT implements Comparable<UserT> {

	@XmlElement(required = true)
	protected String name;

	@XmlElement(required = true)
	protected String defaultFolder;

	@XmlElement(required = true)
	protected String description;

	@XmlElement(required = true)
	protected String email;

	@XmlElement(required = true)
	protected String loginName;

	@XmlElement(required = true)
	protected String loginDomain;

	@XmlElement(required = true)
	protected String osName;

	@XmlElement(required = true)
	protected String osDomain;

	@XmlElement(required = true)
	protected String defaultAcl;

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getDefaultFolder() {
		return this.defaultFolder;
	}

	public void setDefaultFolder(String value) {
		this.defaultFolder = value;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String value) {
		this.description = value;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String value) {
		this.email = value;
	}

	public String getLoginName() {
		return this.loginName;
	}

	public void setLoginName(String value) {
		this.loginName = value;
	}

	public String getLoginDomain() {
		return this.loginDomain;
	}

	public void setLoginDomain(String value) {
		this.loginDomain = value;
	}

	public String getOsName() {
		return this.osName;
	}

	public void setOsName(String value) {
		this.osName = value;
	}

	public String getOsDomain() {
		return this.osDomain;
	}

	public void setOsDomain(String value) {
		this.osDomain = value;
	}

	public String getDefaultAcl() {
		return this.defaultAcl;
	}

	public void setDefaultAcl(String value) {
		this.defaultAcl = value;
	}

	@Override
	public int compareTo(UserT o) {
		if (o == this) { return 0; }
		if (o == null) { return 1; }
		int r = Tools.compare(this.name, o.name);
		if (r != 0) { return r; }
		r = Tools.compare(this.loginDomain, o.loginDomain);
		if (r != 0) { return r; }
		r = Tools.compare(this.loginName, o.loginName);
		if (r != 0) { return r; }
		r = Tools.compare(this.osDomain, o.osDomain);
		if (r != 0) { return r; }
		r = Tools.compare(this.osName, o.osName);
		if (r != 0) { return r; }
		return r;
	}

	@Override
	public String toString() {
		return String
			.format(
				"UserT [name=%s, defaultFolder=%s, description=%s, email=%s, loginName=%s, loginDomain=%s, osName=%s, osDomain=%s, defaultAcl=%s]",
				this.name, this.defaultFolder, this.description, this.email, this.loginName, this.loginDomain,
				this.osName, this.osDomain, this.defaultAcl);
	}
}