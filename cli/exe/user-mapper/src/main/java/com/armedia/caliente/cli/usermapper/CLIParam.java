package com.armedia.caliente.cli.usermapper;

import com.armedia.caliente.cli.MutableParameter;
import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.parser.ParameterWrapper;

public enum CLIParam implements ParameterWrapper {
	//
	dctm_sam(new MutableParameter() //
		.setMaxValueCount(-1) //
		.setValueName("attribute name") //
		.setDescription("The Documentum attribute to attempt to use for mapping directly to the sAMAccountName. "
			+ "Multiple instances of this parameter may be specified and each will be tried in turn "
			+ "(The default mode is to first try user_login_name, then try user_os_name") //
	), //
	ldap_url(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("ldap url") //
		.setDescription("The LDAP URL to bind to") //
	), //
	ldap_binddn(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("ldap dn") //
		.setDescription("The DN to bind to LDAP with") //
	), //
	ldap_pass(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("password") //
		.setDescription("The password to bind to LDAP with") //
	), //
	ldap_basedn(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for both users and groups (SUB scope)") //
	), //
	ldap_user_basedn(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for users (SUB scope)") //
	), //
	ldap_group_basedn(new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for groups (SUB scope)") //
	), //
	ldap_on_demand(new MutableParameter() //
		.setDescription("Execute LDAP queries on demand vs. batched up front (default is batched up front)") //
	), //
	add_docbase(new MutableParameter() //
		.setDescription("Add the docbase name to the files generated (use for running multiple "
			+ "instances at once in the same directory)") //
	), //
		//
	;

	private final Parameter parameter;

	private CLIParam(MutableParameter parameter) {
		this.parameter = MutableParameter.initOptionName(this, parameter).freezeCopy();
	}

	@Override
	public Parameter getParameter() {
		return this.parameter;
	}
}