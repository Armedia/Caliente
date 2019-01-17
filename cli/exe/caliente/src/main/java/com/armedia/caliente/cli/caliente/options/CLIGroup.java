package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;

public class CLIGroup {

	public static final OptionGroup BASE = new OptionGroupImpl("Base") //
		.add(CLIParam.engine) //
		.add(CLIParam.lib) //
		.add(CLIParam.log) //
		.add(CLIParam.log_cfg) //
		.add(CLIParam.log_dir) //
	;

	public static final OptionGroup STORE = new OptionGroupImpl("Data Store") //
		.add(CLIParam.data) //
		.add(CLIParam.streams) //
		.add(CLIParam.organizer) //
		.add(CLIParam.db) //
	;

	public static final OptionGroup MAIL = new OptionGroupImpl("SMTP") //
		.add(CLIParam.mail_auth) //
		.add(CLIParam.mail_bcc) //
		.add(CLIParam.mail_cc) //
		.add(CLIParam.mail_from) //
		.add(CLIParam.mail_host) //
		.add(CLIParam.mail_password) //
		.add(CLIParam.mail_port) //
		.add(CLIParam.mail_ssl) //
		.add(CLIParam.mail_to) //
		.add(CLIParam.mail_user) //
	;

	public static final OptionGroup IMPORT_EXPORT_COMMON = new OptionGroupImpl("Common Export/Import") //
		.add(CLIParam.error_count) //
		.add(CLIParam.exclude_types) //
		.add(CLIParam.external_metadata) //
		.add(CLIParam.filter) //
		.add(CLIParam.manifest_types) //
		.add(CLIParam.no_renditions) //
		.add(CLIParam.no_versions) //
		.add(CLIParam.skip_content) //
		.add(CLIParam.threads) //
		.add(CLIParam.transformations) //
	;

	public static final OptionGroup EXPORT_COMMON = new OptionGroupImpl("Common Export") //
		.addFrom(CLIGroup.IMPORT_EXPORT_COMMON) //
		.add(CLIParam.direct_fs) //
		.add(CLIParam.manifest_outcomes_export) //
		.add(CLIParam.source) //
	;

	public static final OptionGroup IMPORT_COMMON = new OptionGroupImpl("Common Import") //
		.addFrom(CLIGroup.IMPORT_EXPORT_COMMON.getOptions()) //
		.add(CLIParam.filename_map) //
		.add(CLIParam.group_map) //
		.add(CLIParam.manifest_outcomes_import) //
		.add(CLIParam.no_filename_map) //
		.add(CLIParam.restrict_to) //
		.add(CLIParam.role_map) //
		.add(CLIParam.target) //
		.add(CLIParam.trim_path) //
		.add(CLIParam.user_map) //
		.add(CLIParam.validate_requirements) //
	;

	public static final OptionGroup COUNT_COMMON = new OptionGroupImpl("Common Count") //
		.add(CLIParam.count_empty) //
		.add(CLIParam.count_exclude) //
		.add(CLIParam.count_include) //
		.add(CLIParam.count_hidden) //
		.add(CLIParam.non_recursive) //
		.add(CLIParam.no_versions) //
		.add(CLIParam.count_private) //
		.add(CLIParam.threads) //
	;

	public static final OptionGroup AUTHENTICATION = new OptionGroupImpl("Basic Authentication") //
		.add(CLIParam.user) //
		.add(CLIParam.password) //
	;

	public static final OptionGroup CONNECTION = new OptionGroupImpl("Common Connection") //
		.addFrom(CLIGroup.AUTHENTICATION) //
		.add(CLIParam.server) //
	;

	public static final OptionGroup DOMAIN_CONNECTION = new OptionGroupImpl("Domain Connection") //
		.addFrom(CLIGroup.CONNECTION) //
		.add(CLIParam.domain) //
	;
}