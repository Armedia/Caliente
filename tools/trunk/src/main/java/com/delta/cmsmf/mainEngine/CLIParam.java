package com.delta.cmsmf.mainEngine;

import org.apache.commons.cli.Option;

import com.delta.cmsmf.cfg.Setting;

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
	log_name(null, true, "The base name of the log file to use instead of the default (cmsmf-${action})"),
	log4j(null, true, "The Log4j configuration (XML format) to use instead of the default (overrides --log_name)"),
	threads(null, true, "The number of threads to use while importing or exporting"),
	special_users(Setting.SPECIAL_USERS, true,
		"The special users that should not be imported into the target instance (comma-separated)"),
	special_groups(Setting.SPECIAL_GROUPS, true,
		"The special users that should not be imported into the target instance (comma-separated)"),
	special_types(Setting.SPECIAL_TYPES, true,
		"The special types that should not be imported into the target instance (comma-separated)"),
	batch_size(Setting.EXPORT_BATCH_SIZE, true, "The batch size to use when exporting objects from Documentum"),
	post_process(Setting.POST_PROCESS_IMPORT, false, "Whether to post-process the imported content"),
	predicate(Setting.EXPORT_PREDICATE, true, "The DQL 'from-where' predicate to use for exporting"),
	buffer(Setting.CONTENT_READ_BUFFER_SIZE, true, "The size of the read buffer"),
	streams(Setting.STREAMS_DIRECTORY, true, "The Streams directory to use"),
	content(Setting.CONTENT_DIRECTORY, true, "The Content directory to use"),
	compress(Setting.COMPRESSDATA_FLAG, false, "Enable compression for the data exported (GZip)"),
	attributes(Setting.OWNER_ATTRIBUTES, true, "The attributes to check for"),
	errorCount(Setting.IMPORT_MAX_ERRORS, true, "The number of errors to accept before aborting an import"),
	defaultPassword(Setting.DEFAULT_USER_PASSWORD, true,
		"The default password to use for users being copied over (leave blank to use the same login name)"),
	mailTo(Setting.MAIL_RECIPIENTS, true, "The comma-separated list of recipients for the status e-mails"),
	mailFrom(Setting.MAIL_FROM_ADDX, true, "The e-mail address to use as the sender for the status e-mails"),
	smtpHost(Setting.MAIL_SMTP_HOST, true, "The SMTP server through which e-mail must be sent"),
	skip_users(Setting.SKIP_USERS, false, "Skip exporting users"),
	skip_groups(Setting.SKIP_GROUPS, false, "Skip exporting groups"),
	skip_acls(Setting.SKIP_ACLS, false, "Skip exporting acls");

	public final Setting property;
	public final Option option;

	private CLIParam(Setting property, boolean hasParameter, boolean required, String description) {
		this.property = property;
		this.option = new Option(null, name().replace('_', '-'), hasParameter, description);
		this.option.setRequired(required);
	}

	private CLIParam(Setting property, boolean hasParameter, String description) {
		this(property, hasParameter, false, description);
	}
}