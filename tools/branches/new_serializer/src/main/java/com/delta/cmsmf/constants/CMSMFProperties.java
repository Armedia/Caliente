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
	CONTENT_READ_BUFFER_SIZE("content_read_buffer_size");

	public final String name;

	private CMSMFProperties(String name) {
		this.name = name;
	}
}