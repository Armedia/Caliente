package com.armedia.caliente.cli.usermapper;

import java.util.function.Supplier;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;

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