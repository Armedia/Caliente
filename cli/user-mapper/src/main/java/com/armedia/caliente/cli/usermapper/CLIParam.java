package com.armedia.caliente.cli.usermapper;

import java.util.Set;

import com.armedia.caliente.cli.parser.MutableParameter;
import com.armedia.caliente.cli.parser.Parameter;

public enum CLIParam implements Parameter {
	//
	dctm_sam(new MutableParameter() //
		.setValueCount(-1) //
		.setValueOptional(false) //
		.setValueName("attribute name") //
		.setDescription("The Documentum attribute to attempt to use for mapping directly to the sAMAccountName. "
			+ "Multiple instances of this parameter may be specified and each will be tried in turn "
			+ "(The default mode is to first try user_login_name, then try user_os_name") //
	), //
	ldap_url(new MutableParameter() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap url") //
		.setDescription("The LDAP URL to bind to") //
	), //
	ldap_binddn(new MutableParameter() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap dn") //
		.setDescription("The DN to bind to LDAP with") //
	), //
	ldap_pass(new MutableParameter() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("password") //
		.setDescription("The password to bind to LDAP with") //
	), //
	ldap_basedn(new MutableParameter() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for both users and groups (SUB scope)") //
	), //
	ldap_user_basedn(new MutableParameter() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for users (SUB scope)") //
	), //
	ldap_group_basedn(new MutableParameter() //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("ldap dn") //
		.setDescription("The Base DN to search LDAP for groups (SUB scope)") //
	), //
	ldap_on_demand(new MutableParameter() //
		.setValueCount(0) //
		.setDescription("Execute LDAP queries on demand vs. batched up front (default is batched up front)") //
	), //
	add_docbase(new MutableParameter() //
		.setValueCount(0) //
		.setDescription("Add the docbase name to the files generated (use for running multiple "
			+ "instances at once in the same directory)") //
	), //
		//
	;

	private final Parameter parameter;

	private CLIParam(MutableParameter parameter) {
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

	@Override
	public Set<String> getAllowedValues() {
		return this.parameter.getAllowedValues();
	}

	@Override
	public boolean isEqual(Parameter other) {
		return this.parameter.isEqual(other);
	}
}