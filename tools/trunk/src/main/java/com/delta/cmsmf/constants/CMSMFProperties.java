package com.delta.cmsmf.constants;

public enum CMSMFProperties {
	//
	COMPRESSDATA_FLAG("cmsmf.app.compressdata.flag"),
	EXPORT_QUERY_PREDICATE("cmsmf.app.export.query.predicate"),
	IMPORT_ERRORCOUNT_THRESHOLD("cmsmf.app.import.errorcount.threshold"),
	CONTENT_DIRECTORY("cmsmf.app.importexport.content.directory"),
	STREAMS_DIRECTORY("cmsmf.app.importexport.directory"),
	OPERATING_MODE("cmsmf.app.importexport.mode"),
	DEFAULT_USER_PASSWORD("cmsmf.app.inlinepassworduser.passwordvalue"),
	// CMSMF_APP_PASSWORDS_ENCRYPTED_FLAG("cmsmf.app.passwords.encrypted.flag"),
	OWNER_ATTRIBUTES("cmsmf.app.repositoryowner.attributestocheck"),
	TEST_MODE("cmsmf.app.run_mode"),
	CONTENT_READ_BUFFER_SIZE("content_read_buffer_size"),
	DOCBASE_NAME("docbase.name"),
	DOCBASE_USER("docbase.user"),
	DOCBASE_PASSWORD("docbase.password");

	public final String name;

	private CMSMFProperties(String name) {
		this.name = name;
	}
}