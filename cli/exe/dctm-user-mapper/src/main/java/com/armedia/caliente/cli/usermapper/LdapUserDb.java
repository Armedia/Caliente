/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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