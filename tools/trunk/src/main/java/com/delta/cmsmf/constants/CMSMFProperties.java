package com.delta.cmsmf.constants;

public enum CMSMFProperties {
	//
	COMPRESSDATA_FLAG("cmsmf.app.compressdata.flag"),
	EXPORT_QUERY_PREDICATE("cmsmf.app.export.query.predicate"),
	IMPORT_MAX_ERRORS("cmsmf.app.import.errorcount.threshold"),
	CONTENT_DIRECTORY("cmsmf.app.importexport.content.directory"),
	STREAMS_DIRECTORY("cmsmf.app.importexport.directory"),
	DEFAULT_USER_PASSWORD("cmsmf.app.inlinepassworduser.passwordvalue"),
	// CMSMF_APP_PASSWORDS_ENCRYPTED_FLAG("cmsmf.app.passwords.encrypted.flag"),
	OWNER_ATTRIBUTES("cmsmf.app.repositoryowner.attributestocheck"),
	CONTENT_READ_BUFFER_SIZE("content_read_buffer_size"),
	MAIL_RECIPIENTS("cmsmf.app.mail.recipients"),
	MAIL_FROM_ADDX("cmsmf.app.mail.fromAddress"),
	MAIL_SMTP_HOST("cmsmf.app.mail.smtphost"),
	SKIP_ACLS("cmsmf.app.export.skip.acls"),
	SKIP_USERS("cmsmf.app.export.skip.users"),
	SKIP_GROUPS("cmsmf.app.export.skip.groups");

	public final String name;

	private CMSMFProperties(String name) {
		this.name = name;
	}
}