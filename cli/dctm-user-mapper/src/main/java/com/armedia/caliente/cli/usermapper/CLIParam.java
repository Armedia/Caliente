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

import java.util.function.Supplier;

import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionImpl;

public enum CLIParam implements Supplier<Option> {
	//
	dctm_sam(
		new OptionImpl() //
			.setMaxArguments(-1) //
			.setArgumentName("attribute-name") //
			.setDescription("The Documentum attribute to attempt to use for mapping directly to the sAMAccountName. "
				+ "Multiple instances of this option may be specified and each will be tried in turn "
				+ "(The default mode is to first try user_login_name, then try user_os_name") //
	), //
	ldap_url(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("ldap-url") //
			.setDescription("The LDAP URL to bind to") //
	), //
	ldap_binddn(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("ldap-dn") //
			.setDescription("The DN to bind to LDAP with") //
	), //
	ldap_pass(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("password") //
			.setDescription("The password to bind to LDAP with") //
	), //
	ldap_basedn(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("ldap-dn") //
			.setDescription("The Base DN to search LDAP for both users and groups (SUB scope)") //
	), //
	ldap_user_basedn(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("ldap-dn") //
			.setDescription("The Base DN to search LDAP for users (SUB scope)") //
	), //
	ldap_group_basedn(
		new OptionImpl() //
			.setMinArguments(1) //
			.setMaxArguments(1) //
			.setArgumentName("ldap-dn") //
			.setDescription("The Base DN to search LDAP for groups (SUB scope)") //
	), //
	ldap_on_demand(
		new OptionImpl() //
			.setDescription("Execute LDAP queries on demand vs. batched up front (default is batched up front)") //
	), //
	add_docbase(
		new OptionImpl() //
			.setDescription("Add the docbase name to the files generated (use for running multiple "
				+ "instances at once in the same directory)") //
	), //
		//
	;

	private final Option option;

	private CLIParam(OptionImpl parameter) {
		this.option = OptionImpl.initOptionName(this, parameter);
	}

	@Override
	public Option get() {
		return this.option;
	}
}