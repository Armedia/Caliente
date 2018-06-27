package com.armedia.caliente.cli.caliente.cfg;

import com.armedia.caliente.cli.EnumValueFilter;
import com.armedia.caliente.cli.IntegerValueFilter;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionWrapper;
import com.armedia.caliente.cli.StringValueFilter;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.store.CmfType;

public enum CLIParam implements OptionWrapper {
	//

	//
	// Second pass
	//
	dfc( //
		DfcLaunchHelper.DFC_LOCATION //
	), //
	dfc_prop( //
		DfcLaunchHelper.DFC_PROPERTIES //
	), //
	dctm( //
		DfcLaunchHelper.DFC_DOCUMENTUM //
	), //
	non_recursive( //
		new OptionImpl() //
			.setDescription("Turn off counter recursion (i.e. to count a single folder without descending)") //
	), //
	count_empty( //
		new OptionImpl() //
			.setDescription("Enable reporting of empty folders (i.e. folders with 0 non-folder children)") //
	), //
	count_private( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setDescription(
				"For any cabinets encountered, evaluate their private status and include or exclude the private ones. The special value 'any' (default) causes the status to not be taken into account") //
			.setDefault("any") //
			.setValueFilter(new StringValueFilter( //
				false, // Case-insensitive
				"true", "yes", "on", "1", "false", "off", "no", "0", "any" //
		)) //
	), //
	count_hidden( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setDescription(
				"For any folders encountered, evaluate their hidden status and include or exclude the hidden ones. The special value 'any' (default) causes the status to not be taken into account") //
			.setDefault("any") //
			.setValueFilter(new StringValueFilter( //
				false, // Case-insensitive
				"true", "yes", "on", "1", "false", "off", "no", "0", "any" //
		)) //
	), //
	count_include( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setDescription(
				"Include the folder in the count (defaults to only these, may be specified multiple times) - path or object ID is valid") //
			.setArgumentName("path-or-id") //
	), //
	count_exclude( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setDescription(
				"Exclude the folder in the count (defaults to ALL except these, may be specified multiple times) - path or object ID is valid") //
			.setValueSep(',') //
			.setArgumentName("path-or-id") //
	), //
	exclude_types(
		Setting.CMF_EXCLUDE_TYPES, //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setDescription("The list of object types to be ignored during the operation (comma-separated)") //
			.setValueFilter(new EnumValueFilter<>(false, CmfType.class)) //
			.setValueSep(',') //
			.setArgumentName("type") //
	), //
	cmf_import_target_location(
		Setting.CMF_IMPORT_TARGET_LOCATION, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("target-path") //
			.setDescription("The path location into which to import the contents") //
	), //
	cmf_import_trim_prefix(
		Setting.CMF_IMPORT_TRIM_PREFIX, //
		new OptionImpl() //
			.setArgumentLimits(0, 1) //
			.setArgumentName("number") //
			.setDescription("The number of leading path components to trim from the content being imported") //
	), //
	special_users(
		Setting.SPECIAL_USERS, //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("user") //
			.setDescription("The special users that should not be imported into the target instance") //
			.setValueSep(',') //
	), //
	special_groups(
		Setting.SPECIAL_GROUPS, //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("group") //
			.setDescription("The special users that should not be imported into the target instance") //
			.setValueSep(',') //
	), //
	special_types(
		Setting.SPECIAL_TYPES, //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("type") //
			.setDescription("The special types that should not be imported into the target instance") //
			.setValueSep(',') //
	), //
	batch_size( //
		null, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("batch-size") //
			.setDescription("The batch size to use when exporting objects from Documentum") //
	), //
	post_process( //
		null, //
		new OptionImpl() //
			.setDescription("Whether to post-process the imported content") //
	), //
	shpt_source_prefix(
		Setting.SHPT_SOURCE_PREFIX, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("prefix") //
			.setDescription("The prefix to pre-pend to Sharepoint source paths (i.e. /sites is the default)") //
			.setDefault("/sites") //
	), //
	owner_attributes(
		Setting.OWNER_ATTRIBUTES, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("attribute-name") //
			.setDescription("The owner_attributes to check for") //
	), //
	errorCount(
		Setting.IMPORT_MAX_ERRORS, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("number") //
			.setDescription("The number of errors to accept before aborting an import") //
	), //
	default_password(
		Setting.DEFAULT_USER_PASSWORD, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("password") //
			.setDescription(
				"The default password to use for users being copied over (the default is to use the same login name)") //
	), //
	manifest_types(
		null, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("types") //
			.setValueFilter(new EnumValueFilter<>(false, CmfType.class)) //
			.setDescription("The object types to include in the manifest (not specified = all types)") //
	), //
	manifest_outcomes(
		null,
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("outcomes") //
			// TODO: This parameter changes depending on the mode of operation
			.setValueFilter(new EnumValueFilter<>(false, ExportResult.class)) //
			// .setValueFilter(new EnumValueFilter<>(false, ImportResult.class)) //
			.setDescription("The outcomes to include in the manifest (not specified = all outcomes)") //
	), //
	mail_to(
		Setting.MAIL_TO,
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("recipient") //
			// .setValueFilter(new EmailValueFilter()) //
			.setDescription("The primary recipient(s) for the status e-mails") //
	), //
	mail_cc(
		Setting.MAIL_CC,
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("recipient") //
			// .setValueFilter(new EmailValueFilter()) //
			.setDescription("The recipient(s) to CC in the status e-mails") //
	), //
	mail_bcc(
		Setting.MAIL_CC,
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("recipient") //
			// .setValueFilter(new EmailValueFilter()) //
			.setDescription("The recipient(s) to BCC in the status e-mails") //
	), //
	mail_from(
		Setting.MAIL_FROM_ADDX, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("sender") //
			// .setValueFilter(new EmailValueFilter()) //
			.setDescription("The e-mail address to use as the sender for the status e-mails") //
	), //
	mail_host(
		Setting.MAIL_SMTP_HOST, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("hostname-or-ip") //
			// .setValueFilter(new InetAddressValueFilter()) //
			.setDescription("The SMTP server through which e-mail must be sent") //
	), //
	mail_port(
		Setting.MAIL_SMTP_HOST, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("port") //
			.setValueFilter(new IntegerValueFilter(1, true, 65535, true)) //
			.setDescription("The port SMTP server is listening on") //
	), //
	skip_users(
		null, //
		new OptionImpl() //
			.setDescription("Skip exporting users") //
	), //
	skip_groups(
		null, //
		new OptionImpl() //
			.setDescription("Skip exporting groups") //
	), //
	skip_acls(
		null, //
		new OptionImpl() //
			.setDescription("Skip exporting acls") //
	), //
	skip_content( //
		new OptionImpl() //
			.setDescription("Skip importing document contents (only create \"empty\" documents)") //
	), //
	no_renditions( //
		new OptionImpl() //
			.setDescription(
				"Only include the main content stream for documents (and only the first page where multipage is supported)") //
	), //
	no_versions( //
		new OptionImpl() //
			.setDescription("Only include the latest (current) version of documents") //
	), //
	job_name( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setDescription("The name of the synchronization job this export is to define") //
	), //
	reset_job( //
		new OptionImpl() //
			.setDescription("Reset any existing synchronization job settings with this export's") //
	), //
	content_model( //
		new OptionImpl() //
			.setArgumentName("content-model-file") //
			.setArgumentLimits(0, -1) //
			.setDescription("The XML files that make up the Alfresco content model to use on import") //
	), //
	user_map( //
		Setting.USER_MAP, //
		new OptionImpl() //
			.setArgumentName("user-map-file") //
			.setDefault("usermap.xml") //
			.setArgumentLimits(1) //
			.setDescription("The Properties (XML) file that contains the user name mappings to apply") //
	), //
	group_map( //
		Setting.GROUP_MAP, //
		new OptionImpl() //
			.setArgumentName("group-map-file") //
			.setDefault("groupmap.xml") //
			.setArgumentLimits(1) //
			.setDescription("The Properties (XML) file that contains the group name mappings to apply") //
	), //
	role_map( //
		Setting.ROLE_MAP,
		new OptionImpl() //
			.setArgumentName("role-map-file") //
			.setDefault("rolemap.xml") //
			.setArgumentLimits(1) //
			.setDescription("The Properties (XML) file that contains the role name mappings to apply") //
	), //
	no_filename_map( //
		new OptionImpl() //
			.setDescription("Disable the use of the filename map (even if the default map exists)") //
	), //
	validate_requirements( //
		new OptionImpl() //
			.setDescription(
				"Activate the validation of an object's requirements' import success during object import (object is skipped if any of its requirements fails to import properly)") //
	), //
	filename_map(
		Setting.FILENAME_MAP, //
		new OptionImpl() //
			.setArgumentName("filename-map-file") //
			.setDefault("filenamemap.xml") //
			.setArgumentLimits(1) //
			.setDescription("The Properties (XML) file that contains the static filename mappings to be applied") //

	), //
	object_store_config( //
		new OptionImpl() //
			.setArgumentName("config-file") //
			.setArgumentLimits(1) //
			.setDescription("The properties file to use for Object Store DB configuration") //
	), //
	content_store_config( //
		new OptionImpl() //
			.setArgumentName("config-file") //
			.setArgumentLimits(1) //
			.setDescription("The properties file to use for Content Store DB configuration") //
	), //
	copy_content( //
		new OptionImpl() //
			.setDescription("Enable the copying of content for the Local engine") //
	), //
	ignore_empty_folders( //
		new OptionImpl() //
			.setDescription("Enable the copying of content for the Local engine") //
	), //
		//
	;

	public final Setting property;
	public final OptionImpl option;

	private CLIParam(OptionImpl option) {
		this(null, option);
	}

	private CLIParam(Setting property, OptionImpl option) {
		if (option.getLongOpt() == null) {
			option.setLongOpt(name().replace('_', '-'));
		}
		this.option = option;
		this.property = property;
	}

	private CLIParam(Option option) {
		this(null, option);
	}

	private CLIParam(Setting property, Option option) {
		this.option = new OptionImpl(option);
		this.property = property;
	}

	@Override
	public Option getOption() {
		return this.option;
	}

	public OptionImpl getOptionImpl() {
		return this.option;
	}
}