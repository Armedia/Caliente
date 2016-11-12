package com.armedia.caliente.cli.usermapper;

import com.armedia.caliente.cli.parser.MutableParameterDefinition;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public enum CLIParam implements ParameterDefinition {
	//
	lib(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("directory") //
		.setDescription(
			"The directory which contains extra classes (JARs, ZIPs or a classes directory) that should be added to the classpath") //
	), //
	dfc(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("dfc install location") //
		.setDescription("The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)") //
	), //
	dfc_prop(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("dfc.properties location") //
		.setDescription("The dfc.properties file to use instead of the default") //
	), //
	dctm(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("directory") //
		.setDescription("The user's local Documentum path (i.e. instead of DOCUMENTUM)") //
	), //
	docbase(new MutableParameterDefinition() //
		.setRequired(true) //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("repository") //
		.setDescription("The Documentum repostory name to connect to") //
	), //
	dctm_user(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("username") //
		.setDescription("The username to connect to Documentum with") //
	), //
	dctm_pass(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("password") //
		.setDescription("The password to connect to Documentum with (may be encrypted)") //
	), //
	dctm_sam(new MutableParameterDefinition() //
		.setValueCount(-1) //
		.setValueOptional(false) //
		.setValueName("attribute name") //
		.setDescription("The Documentum attribute to attempt to use for mapping directly to the sAMAccountName. "
			+ "Multiple instances of this parameter may be specified and each will be tried in turn "
			+ "(The default mode is to first try user_login_name, then try user_os_name") //
	), //
	ldap_url(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap url") //
		.setDescription("The LDAP URL to bind to") //
	), //
	ldap_binddn(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap dn") //
		.setDescription("The DN to bind to LDAP with") //
	), //
	ldap_basedn(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for both users and groups (SUB scope)") //
	), //
	ldap_user_basedn(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for users (SUB scope)") //
	), //
	ldap_group_basedn(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for groups (SUB scope)") //
	), //
	ldap_pass(new MutableParameterDefinition() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("password") //
		.setDescription("The password to bind to LDAP with") //
	), //
	ldap_on_demand(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Execute LDAP queries on demand vs. batched up front (default is batched up front)") //
	), //
	add_docbase(new MutableParameterDefinition() //
		.setValueCount(0) //
		.setDescription("Add the docbase name to the files generated (use for running multiple "
			+ "instances at once in the same directory)") //
	), //
		//
	;

	private final ParameterDefinition parameter;

	private CLIParam(MutableParameterDefinition parameter) {
		String name = name();
		if (name.length() == 1) {
			// If we decide that the name of the option will be a single character, we use that
			parameter.setShortOpt(name.charAt(0));
		} else if (parameter.getLongOpt() == null) {
			// Otherwise, use the name replacing underscores with dashes
			parameter.setLongOpt(name().replace('_', '-'));
		}
		this.parameter = parameter.clone();
	}

	@Override
	public final String getKey() {
		return this.parameter.getKey();
	}

	@Override
	public final boolean isRequired() {
		return this.parameter.isRequired();
	}

	@Override
	public final String getDescription() {
		return this.parameter.getDescription();
	}

	@Override
	public final String getLongOpt() {
		return this.parameter.getLongOpt();
	}

	@Override
	public final Character getShortOpt() {
		return this.parameter.getShortOpt();
	}

	@Override
	public final Character getValueSep() {
		return this.parameter.getValueSep();
	}

	@Override
	public final String getValueName() {
		return this.parameter.getValueName();
	}

	@Override
	public final int getValueCount() {
		return this.parameter.getValueCount();
	}

	@Override
	public final boolean isValueOptional() {
		return this.parameter.isValueOptional();
	}
}