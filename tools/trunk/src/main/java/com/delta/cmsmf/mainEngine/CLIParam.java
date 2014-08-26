package com.delta.cmsmf.mainEngine;

import org.apache.commons.cli.Option;

import com.delta.cmsmf.properties.CMSMFProperties;

public enum CLIParam {
	//
	help(null, false, "This help message"),
	test(null, false, "Enable test mode"),
	cfg(null, true, "The configuration file to use"),
	dfc(null, true, "The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)"),
	dctm(null, true, "The user's local Documentum path (i.e. instead of DOCUMENTUM)"),
	mode(null, true, true, "The mode of operation, either 'encrypt', 'decrypt', 'import' or 'export'"),
	docbase(null, true, "The docbase name to connect to"),
	user(null, true, "The username to connect with"),
	password(null, true, "The password to connect with"),
	log4j(null, true, "The Log4j configuration (XML format) to use instead of the default"),
	post_process(CMSMFProperties.POST_PROCESS_IMPORT, false, "Whether to post-process the imported content"),
	predicate(CMSMFProperties.EXPORT_PREDICATE, true, "The DQL 'from-where' predicate to use for exporting"),
	buffer(CMSMFProperties.CONTENT_READ_BUFFER_SIZE, true, "The size of the read buffer"),
	streams(CMSMFProperties.STREAMS_DIRECTORY, true, "The Streams directory to use"),
	content(CMSMFProperties.CONTENT_DIRECTORY, true, "The Content directory to use"),
	compress(CMSMFProperties.COMPRESSDATA_FLAG, false, "Enable compression for the data exported (GZip)"),
	attributes(CMSMFProperties.OWNER_ATTRIBUTES, true, "The attributes to check for"),
	errorCount(CMSMFProperties.IMPORT_MAX_ERRORS, true, "The number of errors to accept before aborting an import"),
	defaultPassword(CMSMFProperties.DEFAULT_USER_PASSWORD, true,
		"The default password to use for users being copied over (leave blank to use the same login name)"),
		mailTo(CMSMFProperties.MAIL_RECIPIENTS, true, "The comma-separated list of recipients for the status e-mails"),
		mailFrom(CMSMFProperties.MAIL_FROM_ADDX, true, "The e-mail address to use as the sender for the status e-mails"),
		smtpHost(CMSMFProperties.MAIL_SMTP_HOST, true, "The SMTP server through which e-mail must be sent"),
		skip_users(CMSMFProperties.SKIP_USERS, false, "Skip exporting users"),
		skip_groups(CMSMFProperties.SKIP_GROUPS, false, "Skip exporting groups"),
		skip_acls(CMSMFProperties.SKIP_ACLS, false, "Skip exporting acls");

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