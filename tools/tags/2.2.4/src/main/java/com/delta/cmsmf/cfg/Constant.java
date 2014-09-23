package com.delta.cmsmf.cfg;

import com.documentum.fc.common.IDfTime;

/**
 * The Class Constant. This class contains various constant values used throughout the CMSMF
 * application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class Constant {

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

	/**
	 * The from and where clause of the export query that runs periodically. The application will
	 * combine the select clause listed above with this from and where clauses to build the complete
	 * dql query. Please note that this clause will be ignored when the export is running in the
	 * adhoc mode. In that case the from and where clauses are specified in the properties file.
	 */
	public static final String DEFAULT_PREDICATE = "from dm_sysobject where (TYPE(\"dm_folder\") or TYPE(\"dm_document\")) "
		+ "and not folder('/System', descend)"; // and r_modify_date >= DATE('XX_PLACE_HOLDER_XX')";

	/** The file name and path of the properties file used in CMSMF application. */
	public static final String FULLY_QUALIFIED_CONFIG_FILE_NAME = "cmsmf.properties";

	// Time patterns
	/** The date and time pattern commonly used in documentum repository. */
	public static final String LAST_EXPORT_DATETIME_PATTERN = IDfTime.DF_TIME_PATTERN26;

	/** The "sql-neutral" date and time pattern that will be fed for SQL updates to date attributes */
	public static final String SQL_DATETIME_PATTERN = "yyyy-mm-dd hh:mi:ss";

	/** The "sql-neutral" date and time pattern that will be fed for SQL updates to date attributes */
	public static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	/** The date and time pattern used in oracle sql query. */
	public static final String ORACLE_DATETIME_PATTERN = "YYYY-MM-DD HH24:MI:SS";

	public static final int MSSQL_DATETIME_PATTERN = 120;

	/** The date pattern in which last export date is stored in the repository. EX: 12/15/2010 */
	public static final String LAST_EXPORT_DATE_PATTERN = Constant.JAVA_SQL_DATETIME_PATTERN;

	// CMSMF Last Export Related constants
	public static final String LAST_EXPORT_OBJ_NAME = "cmsmf_last_export";

	public static final String RUN_NOW = "run_now";

	public static final String EXACT_ID = "EXACT_ID";

	public static final String VERSION_LABEL = "VERSION_LABEL";
}