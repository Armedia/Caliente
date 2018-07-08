package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;

public class CLIGroup {

	public static final OptionGroup BASE = new OptionGroupImpl("Base Options") //
		.add(CLIParam.engine) //
		.add(CLIParam.help) //
		.add(CLIParam.lib) //
		.add(CLIParam.log) //
		.add(CLIParam.log_cfg) //
		.add(CLIParam.log_dir) //
	;

	public static final OptionGroup IMPORT_EXPORT_COMMON = new OptionGroupImpl("Common Export/Import Options") //
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

	public static final OptionGroup EXPORT_COMMON = new OptionGroupImpl("Common Export Options") //
		.add(CLIParam.direct_fs) //
		.add(CLIParam.manifest_outcomes_export) //
		.add(CLIParam.source) //
	;

	public static final OptionGroup IMPORT_COMMON = new OptionGroupImpl("Common Import Options") //
		.add(CLIParam.direct_fs) //
		.add(CLIParam.filename_map) //
		.add(CLIParam.group_map) //
		.add(CLIParam.manifest_outcomes_import) //
		.add(CLIParam.no_filename_map) //
		.add(CLIParam.role_map) //
		.add(CLIParam.target) //
		.add(CLIParam.trim_path) //
		.add(CLIParam.user_map) //
		.add(CLIParam.validate_requirements) //
	;

	public static final OptionGroup MAIL = new OptionGroupImpl("SMTP Options") //
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

	public static final OptionGroup STORE = new OptionGroupImpl("Data Store Options") //
		.add(CLIParam.content) //
		.add(CLIParam.content_strategy) //
		.add(CLIParam.db) //
	;

	public static final OptionGroup URL = new OptionGroupImpl("Common URL Options") //
		.add(CLIParam.domain) //
		.add(CLIParam.password) //
		.add(CLIParam.url) //
		.add(CLIParam.user) //
	;
}