package com.delta.cmsmf.constants;

public enum CMSMFProperties {
	//
	CMSMF_APP_COMPRESSDATA_FLAG("cmsmf.app.compressdata.flag"),
	CMSMF_APP_EXPORT_QUERY_PREDICATE("cmsmf.app.export.query.predicate"),
	CMSMF_APP_IMPORT_ERRORCOUNT_THRESHOLD("cmsmf.app.import.errorcount.threshold"),
	CMSMF_APP_IMPORTEXPORT_CONTENT_DIRECTORY("cmsmf.app.importexport.content.directory"),
	CMSMF_APP_IMPORTEXPORT_DIRECTORY("cmsmf.app.importexport.directory"),
	CMSMF_APP_IMPORTEXPORT_MODE("cmsmf.app.importexport.mode"),
	CMSMF_APP_INLINEPASSWORDUSER_PASSWORDVALUE("cmsmf.app.inlinepassworduser.passwordvalue"),
	CMSMF_APP_PASSWORDS_ENCRYPTED_FLAG("cmsmf.app.passwords.encrypted.flag"),
	CMSMF_APP_REPOSITORYOWNER_ATTRIBUTESTOCHECK("cmsmf.app.repositoryowner.attributestocheck"),
	CMSMF_APP_RUN_MODE("cmsmf.app.run_mode"),
	CONTENT_READ_BUFFER_SIZE("content_read_buffer_size"),
	DOCBASE_NAME("docbase.name"),
	DOCBASE_USER("docbase.user"),
	DOCBASE_PASSWORD("docbase.password");

	public final String name;

	private CMSMFProperties(String name) {
		this.name = name;
	}
}