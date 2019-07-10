/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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