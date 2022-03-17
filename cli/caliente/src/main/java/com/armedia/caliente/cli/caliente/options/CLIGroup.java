/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.caliente.options;

import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;

public class CLIGroup {

	public static final OptionGroup BASE = new OptionGroupImpl("Base") //
		.add(CLIParam.engine) //
		.add(CLIParam.lib) //
		.add(CLIParam.log) //
		.add(CLIParam.log_cfg) //
		.add(CLIParam.log_dir) //
		.add(CLIParam.verbose) //
	;

	public static final OptionGroup STORE = new OptionGroupImpl("Data Store") //
		.add(CLIParam.data) //
		.add(CLIParam.db) //
		.add(CLIParam.organizer) //
		.add(CLIParam.streams) //
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
		.add(CLIParam.except_types) //
		.add(CLIParam.external_metadata) //
		.add(CLIParam.filename_map) //
		.add(CLIParam.filter) //
		.add(CLIParam.manifest_types) //
		.add(CLIParam.no_filename_map) //
		.add(CLIParam.no_renditions) //
		.add(CLIParam.no_versions) //
		.add(CLIParam.only_types) //
		.add(CLIParam.retry_count) //
		.add(CLIParam.skip_content) //
		.add(CLIParam.threads) //
		.add(CLIParam.transformations) //
	;

	public static final OptionGroup EXPORT_COMMON = new OptionGroupImpl("Common Export") //
		.addFrom(CLIGroup.IMPORT_EXPORT_COMMON) //
		.add(CLIParam.direct_fs) //
		.add(CLIParam.from) //
		.add(CLIParam.ignore_empty_folders) //
		.add(CLIParam.manifest_outcomes_export) //
		.add(CLIParam.metadata_xml) //
	;

	public static final OptionGroup IMPORT_COMMON = new OptionGroupImpl("Common Import") //
		.addFrom(CLIGroup.IMPORT_EXPORT_COMMON.getOptions()) //
		.add(CLIParam.group_map) //
		.add(CLIParam.manifest_outcomes_import) //
		.add(CLIParam.require_all_parents) //
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
		.add(CLIParam.count_hidden) //
		.add(CLIParam.count_include) //
		.add(CLIParam.count_private) //
		.add(CLIParam.no_versions) //
		.add(CLIParam.non_recursive) //
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