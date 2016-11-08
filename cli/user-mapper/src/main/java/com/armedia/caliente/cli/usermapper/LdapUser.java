package com.armedia.caliente.cli.usermapper;

import com.unboundid.ldap.sdk.DN;

public class LdapUser extends LdapPrincipal {
	private static final long serialVersionUID = 1L;

	private final String login;

	public LdapUser(String name, String guid, DN dn, String login) {
		super(name, guid, dn);
		this.login = login;
	}

	public String getLogin() {
		return this.login;
	}

	@Override
	public String toString() {
		return String.format("LdapUser [name=%s, login=%s guid=%s, dn=%s]", getName(), this.login, getGuid(), getDn());
	}
}