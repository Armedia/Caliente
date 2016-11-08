package com.armedia.caliente.cli.usermapper;

import com.unboundid.ldap.sdk.DN;

public class LdapPrincipal extends BasePrincipal {
	private static final long serialVersionUID = 1L;

	private final DN dn;

	public LdapPrincipal(String name, String guid, DN dn) {
		super(name, guid);
		this.dn = dn;
	}

	public DN getDn() {
		return this.dn;
	}
}