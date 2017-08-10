package com.armedia.caliente.cli.usermapper;

import javax.xml.bind.DatatypeConverter;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResultEntry;

public class LdapGroupDb extends LdapPrincipalDb<LdapGroup> {
	private static final String FILTER = "(&(objectCategory=group)(objectClass=group))";
	private static final String[] ATTRIBUTES = {
		"cn", "sAMAccountName", "objectGUID"
	};

	public LdapGroupDb() {
		super(LdapGroup.class);
	}

	public LdapGroupDb(LDAPConnectionPool pool, boolean onDemand, String baseDn) throws LDAPException {
		super(LdapGroup.class, pool, onDemand, baseDn, LdapGroupDb.FILTER, LdapGroupDb.ATTRIBUTES);
	}

	@Override
	protected LdapGroup buildObject(SearchResultEntry entry) {
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
		return new LdapGroup(cn, DatatypeConverter.printHexBinary(guid).toLowerCase(), dn);
	}
}