package com.delta.cmsmf.constants;

/**
 * The Class CMSMFAppConstants. This class contains various constant values used throughout
 * the CMSMF application.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFAppConstants {

	/**
	 * The buffer size in bytes used to retrieve the content file. This buffer size is used to
	 * read the content from input stream these many bytes at time and writing to output stream.
	 */
	public static final int CONTENT_READ_BUFFER_SIZE = 2048;

	/** The description set to temporary ACLs created during the import process. */
	public static final String CMSMF_TEMP_ACL_DESCRIPTION = "CMSMF Temp ACL";

	/** The string value that is stored in user type attribute for inline users. */
	public static final String USER_SOURCE_INLINE_PASSWORD = "inline password";

	/**
	 * The default value for cmsmf.app.inlinepassworduser.passwordvalue property in
	 * CMSMF_app.properties file.
	 * If this property is missing in the properties file, this default value will be used. If this
	 * default
	 * value is used during inline user creation during import, it will set their password same as
	 * their login id
	 */
	public static final String INLINE_PASSWORD_DEFAULT_VALUE = "sameasloginid";

	/**
	 * The select clause of export query used to export sysobjects. To locate system object using a
	 * dql, we need
	 * several of the attributes present in select clause of the query. Hence this select clause
	 * specified here.
	 */
	public static final String EXPORT_QUERY_SELECT_CLAUSE = "select r_object_id, i_vstamp, r_object_type, r_aspect_name, i_is_replica, i_is_reference ";

	/** The file name and path of the properties file used in CMSMF application. */
	public static final String FULLY_QUALIFIED_CONFIG_FILE_NAME = "config/CMSMF_app.properties";

	// Time patterns
	/** The date and time pattern commonly used in documentum repository. */
	public static final String DCTM_DATETIME_PATTERN = "mm/dd/yyyy hh:mi:ss";

	/** The date and time pattern used in oracle sql query. */
	public static final String ORACLE_DATETIME_PATTERN = "MM/DD/YYYY HH24:MI:SS";

	// Generic documentum Constants
	/** The dm_dbo alias used for ACL domains for system ACLs. */
	public static final String DM_DBO = "dm_dbo";

	public static final String DM_SYSOBJECT_E_ALREADY_LINKED_MESSAGE_ID = "DM_SYSOBJECT_E_ALREADY_LINKED";

}
