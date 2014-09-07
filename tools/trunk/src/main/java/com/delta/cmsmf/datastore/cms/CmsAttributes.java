package com.delta.cmsmf.datastore.cms;

/**
 * The Class CmsAttributes. This class contains various constant values for
 * attribute names and column names used in CMSMF application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CmsAttributes {

	// dm_sysobject Attributes
	/** The r_object_id attribute of dm_sysobject. */
	public static final String R_OBJECT_ID = "r_object_id";

	/** The r_object_type attribute of dm_sysobject. */
	public static final String R_OBJECT_TYPE = "r_object_type";

	/** The r_modify_date attribute of dm_sysobject. */
	public static final String R_MODIFY_DATE = "r_modify_date";

	/** The r_modifier attribute of dm_sysobject. */
	public static final String R_MODIFIER = "r_modifier";

	/** The r_creation_date attribute of dm_sysobject. */
	public static final String R_CREATION_DATE = "r_creation_date";

	/** The r_creator_name attribute of dm_sysobject. */
	public static final String R_CREATOR_NAME = "r_creator_name";

	/** The r_immutable_flag attribute of dm_sysobject. */
	public static final String R_IMMUTABLE_FLAG = "r_immutable_flag";

	/** The i_antecedent_id attribute of dm_sysobject. */
	public static final String I_ANTECEDENT_ID = "i_antecedent_id";

	/** The r_version_label attribute of dm_sysobject. */
	public static final String R_VERSION_LABEL = "r_version_label";

	/** The object_name attribute of dm_sysobject. */
	public static final String OBJECT_NAME = "object_name";

	/** The acl_name attribute of dm_sysobject. */
	public static final String ACL_NAME = "acl_name";

	/** The acl_domain attribute of dm_sysobject. */
	public static final String ACL_DOMAIN = "acl_domain";

	/** The owner_name attribute of dm_sysobject. */
	public static final String OWNER_NAME = "owner_name";

	/** The i_vstamp attribute of dm_sysobject. */
	public static final String I_VSTAMP = "i_vstamp";

	/** The i_folder_id attribute of dm_sysobject. */
	public static final String I_FOLDER_ID = "i_folder_id";

	/** The a_content_type attribute of dm_sysobject. */
	public static final String A_CONTENT_TYPE = "a_content_type";

	/** The i_is_deleted attribute of dm_sysobject. */
	public static final String I_IS_DELETED = "i_is_deleted";

	// dm_folder Attributes
	/** The r_folder_path attribute of dm_folder. */
	public static final String R_FOLDER_PATH = "r_folder_path";

	// dm_cabinet Attributes
	/** The is_private attribute of dm_cabinet. */
	public static final String IS_PRIVATE = "is_private";

	// dm_user Attributes
	/** The user_name attribute of dm_user. */
	public static final String USER_NAME = "user_name";

	/** The user_login_domain attribute of dm_user. */
	public static final String USER_LOGIN_DOMAIN = "user_login_domain";

	/** The user_login_name attribute of dm_user. */
	public static final String USER_LOGIN_NAME = "user_login_name";

	/** The user_source attribute of dm_user. */
	public static final String USER_SOURCE = "user_source";

	/** The user_password attribute of dm_user. */
	public static final String USER_PASSWORD = "user_password";

	/** The home_docbase attribute of dm_user. */
	public static final String HOME_DOCBASE = "home_docbase";

	/** The user_grouop_name attribute of dm_user. */
	public static final String USER_GROUP_NAME = "user_group_name";

	/** The default_folder attribute of dm_user. */
	public static final String DEFAULT_FOLDER = "default_folder";

	// dm_group Attributes
	/** The group_name attribute of dm_group. */
	public static final String GROUP_NAME = "group_name";

	/** The group_display_name attribute of dm_group. */
	public static final String GROUP_DISPLAY_NAME = "group_display_name";

	/** The group_admin attribute of dm_group. */
	public static final String GROUP_ADMIN = "group_admin";

	/** The users_names attribute of dm_group. */
	public static final String USERS_NAMES = "users_names";

	// dm_acl Attributes
	/** The r_accessor_name attribute of dm_acl. */
	public static final String R_ACCESSOR_NAME = "r_accessor_name";

	/** The r_accessor_permit attribute of dm_acl. */
	public static final String R_ACCESSOR_PERMIT = "r_accessor_permit";

	/** The r_accessor_xpermit attribute of dm_acl. */
	public static final String R_ACCESSOR_XPERMIT = "r_accessor_xpermit";

	/** The r_is_group attribute of dm_acl. */
	public static final String R_IS_GROUP = "r_is_group";

	/** The accessor_name_dm_world attribute of dm_acl. */
	public static final String ACCESSOR_NAME_DM_WORLD = "dm_world";

	/** The accessor_name_dm_owner attribute of dm_acl. */
	public static final String ACCESSOR_NAME_DM_OWNER = "dm_owner";

	/** The accessor_name_dm_group attribute of dm_acl. */
	public static final String ACCESSOR_NAME_DM_GROUP = "dm_group";

	// dm_type attributes
	/** The super_name attribute of dm_type. */
	public static final String SUPER_NAME = "super_name";

	/** The attr_count attribute of dm_type. */
	public static final String ATTR_COUNT = "attr_count";

	/** The attr_length attribute of dm_type. */
	public static final String ATTR_LENGTH = "attr_length";

	/** The attr_name attribute of dm_type. */
	public static final String ATTR_NAME = "attr_name";

	/** The attr_repeating attribute of dm_type. */
	public static final String ATTR_REPEATING = "attr_repeating";

	/** The attr_type attribute of dm_type. */
	public static final String ATTR_TYPE = "attr_type";

	/** The start_pos attribute of dm_type. */
	public static final String START_POS = "start_pos";

	// dm_format Attributes
	/** The name attribute of dm_format. */
	public static final String NAME = "name";

	// dmr_content Attributes
	/** The full_format attribute of dmr_content. */
	public static final String FULL_FORMAT = "full_format";

	/** The page attribute of dmr_content. */
	public static final String PAGE = "page";

	/** The page_modifier attribute of dmr_content. */
	public static final String PAGE_MODIFIER = "page_modifier";

	/** The rendition attribute of dmr_content. */
	public static final String RENDITION = "rendition";

	/** The data_ticket attribute of dmr_content. */
	public static final String DATA_TICKET = "data_ticket";

	/** The set_file attribute of dmr_content. */
	public static final String SET_FILE = "set_file";

	/** The set_client attribute of dmr_content. */
	public static final String SET_CLIENT = "set_client";

	/** The set_time attribute of dmr_content. */
	public static final String SET_TIME = "set_time";

	// dm_server_config Attributes
	/** The operator_name attribute of dm_server_config. */
	public static final String OPERATOR_NAME = "operator_name";

	/** The r_install_owner attribute of dm_server_config. */
	public static final String R_INSTALL_OWNER = "r_install_owner";

	/** The r_server_version attribute of dm_server_config. */
	public static final String R_SERVER_VERSION = "r_server_version";

	/** The host_name attribute of dm_server_config. */
	public static final String R_HOST_NAME = "r_host_name";

	// dm_docbase_config Attributes
	/** The r_dbms_name attribute of dm_docbase_config. */
	public static final String R_DBMS_NAME = "r_dbms_name";

	/** The security_mode attribute of dm_docbase_config. */
	public static final String SECURITY_MODE = "security_mode";

	/** The r_docbase_id attribute of dm_docbase_config. */
	public static final String R_DOCBASE_ID = "r_docbase_id";

	// Connection config Attributes
	/** The r_docbase_name attribute of connection config. */
	public static final String R_DOCBASE_NAME = "r_docbase_name";

	// dm_job attributes
	public static final String RUN_NOW = "run_now";

	// Various query collection column names
	/** The groups_names column name of getGroupsNames collection. */
	public static final String GROUPS_NAMES = "groups_names";

	/** The new_object_id column name of result collection returned by create type dql. */
	public static final String NEW_OBJECT_ID = "new_object_ID";

}