package com.armedia.caliente.cli.usermapper;

import javax.xml.bind.DatatypeConverter;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResultEntry;

public class LdapUserDb extends LdapPrincipalDb<LdapUser> {
	private static final String FILTER = "(&(objectCategory=person)(objectClass=user))";
	private static final String[] ATTRIBUTES = {
		"cn", "sAMAccountName", "objectGUID"
	};

	public LdapUserDb() {
		super(LdapUser.class);
	}

	public LdapUserDb(LDAPConnectionPool pool, boolean onDemand, String baseDn) throws LDAPException {
		super(LdapUser.class, pool, onDemand, baseDn, LdapUserDb.FILTER, LdapUserDb.ATTRIBUTES);
	}

	@Override
	protected LdapUser buildObject(SearchResultEntry entry) {
		final String cn = entry.getAttributeValue("cn");
		final DN dn;
		try {
			dn = new DN(entry.getDN());
		} catch (LDAPException e) {
			// This is impossible, since the entry is providing the DN...
			throw new RuntimeException(
				String.format("Invalid DN provided by the LDAP infrastructure: [%s]", entry.getDN()), e);
		}
		final byte[] guid = entry.getAttributeValueBytes("objectGUID");
		final String login = entry.getAttributeValue("sAMAccountName");
		return new LdapUser(cn, DatatypeConverter.printHexBinary(guid).toLowerCase(), dn, login);
	}
}