package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;

public class CLIGroup {

	public static final OptionGroup BASE = new OptionGroupImpl("Base Options") //
		.add(CLIParam.help) //
		.add(CLIParam.lib) //
		.add(CLIParam.log) //
		.add(CLIParam.log_cfg) //
		.add(CLIParam.engine) //
	;

	public static final OptionGroup COMMON = new OptionGroupImpl("Common Engine Options") //
		.add(CLIParam.threads) //
		.add(CLIParam.no_renditions) //
		.add(CLIParam.no_versions) //
		.add(CLIParam.skip_content) //
		.add(CLIParam.exclude_types) //
		.add(CLIParam.transformations) //
		.add(CLIParam.filters) //
		.add(CLIParam.external_metadata) //
	;

	public static final OptionGroup EXPORT_COMMON = new OptionGroupImpl("Common Export Options") //
		.add(CLIParam.direct_fs) //
		.add(CLIParam.source) //
	;

	public static final OptionGroup MAIL = new OptionGroupImpl("SMTP Options") //
		.add(CLIParam.mail_from) //
		.add(CLIParam.mail_to) //
		.add(CLIParam.mail_cc) //
		.add(CLIParam.mail_bcc) //
		.add(CLIParam.mail_host) //
		.add(CLIParam.mail_port) //
		.add(CLIParam.mail_ssl) //
		.add(CLIParam.mail_user) //
		.add(CLIParam.mail_password) //
		.add(CLIParam.mail_auth) //
	;

	public static final OptionGroup STORE = new OptionGroupImpl("Data Store Options") //
		.add(CLIParam.db) //
		.add(CLIParam.content) //
		.add(CLIParam.content_strategy) //
	;

	public static final OptionGroup URL = new OptionGroupImpl("Common URL Options") //
		.add(CLIParam.url) //
		.add(CLIParam.user) //
		.add(CLIParam.password) //
		.add(CLIParam.domain) //
	;
}