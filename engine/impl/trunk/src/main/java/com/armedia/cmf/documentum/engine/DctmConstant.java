package com.armedia.cmf.documentum.engine;


/**
 * The Class Constant. This class contains various constant values used throughout the CMSMF
 * application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmConstant {

	/**
	 * The buffer size in bytes used to retrieve the content file. This buffer size is used to read
	 * the content from input stream these many bytes at time and writing to output stream.
	 */
	public static final int CONTENT_READ_BUFFER_SIZE = 2048;

	/** The description set to temporary ACLs created during the import process. */
	public static final String CMSMF_TEMP_ACL_DESCRIPTION = "CMSMF Temp ACL";

	/** The string value that is stored in user type attribute for inline users. */
	public static final String USER_SOURCE_INLINE_PASSWORD = "inline password";

	/**
	 * The default value for cmsmf.app.inlinepassworduser.passwordvalue property in
	 * CMSMF_app.properties file. If this property is missing in the properties file, this default
	 * value will be used. If this default value is used during inline user creation during import,
	 * it will set their password same as their login id
	 */
	public static final String INLINE_PASSWORD_DEFAULT_VALUE = "sameasloginid";

	/**
	 * The select clause of export query used to export sysobjects. To locate system object using a
	 * dql, we need several of the attributes present in select clause of the query. Hence this
	 * select clause specified here.
	 */
	public static final String EXPORT_QUERY_SELECT_CLAUSE = "select distinct r_object_id";

	/** The file name and path of the properties file used in CMSMF application. */
	public static final String FULLY_QUALIFIED_CONFIG_FILE_NAME = "cmsmf.properties";

	/** The "sql-neutral" date and time pattern that will be fed for SQL updates to date attributes */
	public static final String SQL_DATETIME_PATTERN = "yyyy-mm-dd hh:mi:ss";

	/** The "sql-neutral" date and time pattern that will be fed for SQL updates to date attributes */
	public static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	/** The date and time pattern used in oracle sql query. */
	public static final String ORACLE_DATETIME_PATTERN = "YYYY-MM-DD HH24:MI:SS";

	public static final int MSSQL_DATETIME_PATTERN = 120;

	public static final String RUN_NOW = "run_now";

	public static final String EXACT_ID = "EXACT_ID";

	public static final String VERSION_LABEL = "VERSION_LABEL";
}