package com.armedia.caliente.cli.usermapper;

import com.unboundid.ldap.sdk.DN;

public class LdapGroup extends LdapPrincipal {
	private static final long serialVersionUID = 1L;

	public LdapGroup(String name, String guid, DN dn) {
		super(name, guid, dn);
	}
}