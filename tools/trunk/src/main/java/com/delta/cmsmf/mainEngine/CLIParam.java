package com.delta.cmsmf.mainEngine;

import org.apache.commons.cli.Option;

import com.delta.cmsmf.constants.CMSMFProperties;

enum CLIParam {
	//
	help(null, false, "This help message"),
	test(null, false, "Enable test mode"),
	cfg(null, true, "The configuration file to use"),
	dfc(null, true, "The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)"),
	dctm(null, true, "The user's local Documentum path (i.e. instead of DOCUMENTUM)"),
	mode(null, true, true, "The mode of operation, either 'encrypt', 'import' or 'export'"),
	docbase(null, true, "The docbase name to connect to"),
	user(null, true, "The username to connect with"),
	password(null, true, "The password to connect with"),
	predicate(CMSMFProperties.EXPORT_QUERY_PREDICATE, true, "The DQL Predicate to use for exporting"),
	buffer(CMSMFProperties.CONTENT_READ_BUFFER_SIZE, true, "The size of the read buffer"),
	streams(CMSMFProperties.STREAMS_DIRECTORY, true, "The Streams directory to use"),
	content(CMSMFProperties.CONTENT_DIRECTORY, true, "The Content directory to use"),
	compress(CMSMFProperties.COMPRESSDATA_FLAG, false, "Enable compression for the data exported (GZip)"),
	attributes(CMSMFProperties.OWNER_ATTRIBUTES, true, "The attributes to check for"),
	errorCount(CMSMFProperties.IMPORT_MAX_ERRORS, true, "The number of errors to accept before aborting an import"),
	defaultPassword(CMSMFProperties.DEFAULT_USER_PASSWORD, true,
		"The default password to use for users being copied over (leave blank to use the same login name)");

	public final CMSMFProperties property;
	public final Option option;

	private CLIParam(CMSMFProperties property, boolean hasParameter, boolean required, String description) {
		this.property = property;
		this.option = new Option(null, name().replace('_', '-'), hasParameter, description);
		this.option.setRequired(required);
	}

	private CLIParam(CMSMFProperties property, boolean hasParameter, String description) {
		this(property, hasParameter, false, description);
	}
}