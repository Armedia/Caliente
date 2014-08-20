package com.delta.cmsmf.constants;

/**
 * The Class CMSMFAppConstants. This class contains various constant values used throughout
 * the CMSMF application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFAppConstants {

	/**
	 * The batch size for exporting content from Documentum
	 */
	public static final int BATCH_SIZE = 20000;

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
	 * The select clause of export query used to export sysobjects. To locate system object using a
	 * dql, we
	 * need several of the attributes present in select clause of the query. Hence this select
	 * clause
	 * specified here.
	 */
	public static final String EXPORT_QUERY_SELECT_CLAUSE = "select distinct r_object_id";

	/**
	 * The from and where clause of the export query that runs periodically. The application will
	 * combine the
	 * select clause listed above with this from and where clauses to build the complete dql query.
	 * Please
	 * note that this clause will be ignored when the export is running in the adhoc mode. In that
	 * case the
	 * from and where clauses are specified in the properties file.
	 */
	public static final String DEFAULT_PREDICATE = "from dm_sysobject where (TYPE(\"dm_folder\") or TYPE(\"dm_document\")) "
		+ "and not folder('/System', descend)"; // and r_modify_date >= DATE('XX_PLACE_HOLDER_XX')";

	/** The file name and path of the properties file used in CMSMF application. */
	public static final String FULLY_QUALIFIED_CONFIG_FILE_NAME = "cfg/default.properties";

	// Time patterns
	/** The date and time pattern commonly used in documentum repository. */
	public static final String DCTM_DATETIME_PATTERN = "mm/dd/yyyy hh:mi:ss";

	/** The date and time pattern used in oracle sql query. */
	public static final String ORACLE_DATETIME_PATTERN = "MM/DD/YYYY HH24:MI:SS";

	/** The date pattern in which last export date is stored in the repository. EX: 12/15/2010 */
	public static final String LAST_EXPORT_DATE_PATTERN = "MM/dd/yyyy";

	// Generic documentum Constants
	/** The dm_dbo alias used for ACL domains for system ACLs. */
	public static final String DM_DBO = "dm_dbo";

	/** The Constant DM_SYSOBJECT_E_ALREADY_LINKED_MESSAGE_ID. */
	public static final String DM_SYSOBJECT_E_ALREADY_LINKED_MESSAGE_ID = "DM_SYSOBJECT_E_ALREADY_LINKED";

	/** The Constant DM_SYSOBJECT_E_LINK_PERMIT2_MESSAGE_ID. */
	public static final String DM_SYSOBJECT_E_LINK_PERMIT2_MESSAGE_ID = "DM_SYSOBJECT_E_LINK_PERMIT2";

	// CMSMF Lock File Names
	public static final String EXPORT_LOCK_FILE_NAME = "_cmsmf_export.lck";
	public static final String IMPORT_LOCK_FILE_NAME = "_cmsmf_import.lck";

	// CMSMF Last Export Related constants
	public static final String LAST_EXPORT_SYNC_CABINET = "CMSMF_SYNC";
	public static final String LAST_EXPORT_OBJ_NAME = "cmsmf_last_export";
}
