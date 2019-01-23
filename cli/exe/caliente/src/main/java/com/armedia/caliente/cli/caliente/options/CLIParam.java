package com.armedia.caliente.cli.caliente.options;

import com.armedia.caliente.cli.EnumValueFilter;
import com.armedia.caliente.cli.IntegerValueFilter;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionWrapper;
import com.armedia.caliente.cli.StringValueFilter;
import com.armedia.caliente.cli.caliente.utils.SmtpServer.SslMode;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfType;

public enum CLIParam implements OptionWrapper {
	count_empty( //
		new OptionImpl() //
			.setDescription("Enable reporting of empty folders (i.e. folders with 0 non-folder children)") //
	), //

	count_exclude( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("path-or-id") //
			.setDescription(
				"Exclude the folder in the count (defaults to ALL except these, may be specified multiple times) - path or object ID is valid") //
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
			.setArgumentName("path-or-id") //
			.setDescription(
				"Include the folder in the count (defaults to only these, may be specified multiple times) - path or object ID is valid") //
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

	data( //
		new OptionImpl() //
			.setShortOpt('d') //
			.setArgumentLimits(1) //
			.setArgumentName("base-data-directory") //
			.setRequired(true) //
			.setDescription("The directory to use as the root for all storage (except if --db or --content are used)") //
	), //

	db( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("metadata-directory-or-config") //
			.setDescription(
				"The directory into which the metadata database will be stored, or the XML file that describes the store configuration") //
	), //

	direct_fs( //
		new OptionImpl() //
			.setDescription("Export files to local FS duplicating the CMS's path") //
	), //

	domain( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("domain") //
			.setDescription("The domain the user should authenticate against") //
	), //

	engine( //
		new OptionImpl() //
			.setShortOpt('e') //
			.setRequired(true) //
			.setArgumentLimits(1) //
			.setArgumentName("engine") //
			.setDescription("The ECM engine to use") //
	), //

	error_count( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("number") //
			.setValueFilter(new IntegerValueFilter(-1, null)) //
			.setDefault(-1) //
			.setDescription("The number of errors to accept before aborting an import") //
	), //

	exclude_types( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setValueSep(',') //
			.setArgumentName("object-type") //
			.setValueFilter(new EnumValueFilter<>(false, CmfType.class)) //
			.setDescription("Object types to exclude from processing") //
	), //

	external_metadata( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("external-metadata-file") //
			.setDescription("The external metadata descriptor file") //
	), //

	filename_map( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("filename-map-file") //
			.setDefault("filenamemap.xml") //
			.setDescription("The Properties (XML) file that contains the static filename mappings to be applied") //

	), //

	filter( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("filter-file") //
			.setDescription("The object filter descriptor file") //
	), //

	from( //
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1, -1) //
			.setArgumentName("source-spec") //
			.setDescription(
				"The source specifications identifying which content to extract (%searchKey, @fileref, /path, or query string)") //
	), //

	group_map( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("group-map-file") //
			.setDefault("groupmap.xml") //
			.setDescription("The Properties (XML) file that contains the group name mappings to apply") //
	), //

	help( //
		new OptionImpl() //
			.setShortOpt('h') //
			.setDescription("This help message") //
	), //

	lib( //
		LibLaunchHelper.LIB //
	), //

	log( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("log-name-template") //
			.setDefault(CLIConst.DEFAULT_LOG_FORMAT) //
			.setDescription("The base name of the log file to use (${logName}).") //
	), //

	log_dir( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("log-directory") //
			.setDescription("The directory into which the logs will be output, instead of the default") //
	), //

	log_cfg( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("configuration") //
			.setDescription(
				"The Log4j configuration (XML format) to use instead of the default (can reference ${logName} from --log)") //
	), //

	mail_auth( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("authentication-mode") //
			.setValueFilter(CLIFilters.MAIL_AUTH_FILTER) //
			.setDescription("The authentication mode to use when connecting to the SMTP host") //
	), //

	mail_bcc( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setValueSep(',') //
			.setArgumentName("email") //
			.setValueFilter(CLIFilters.EMAIL_FILTER) //
			.setDescription("Blind Carbon Copy Recipients for the status e-mail") //
	), //

	mail_cc( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setValueSep(',') //
			.setArgumentName("email") //
			.setValueFilter(CLIFilters.EMAIL_FILTER) //
			.setDescription("Carbon Copy Recipients for the status e-mail") //
	), //

	mail_from( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("email") //
			.setValueFilter(CLIFilters.EMAIL_FILTER) //
			.setDescription("Sender for the status e-mail") //
	), //

	mail_host( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setDefault("127.0.0.1") //
			.setValueFilter(CLIFilters.INET_ADDX_FILTER) //
			.setDescription("SMTP host to post the status e-mail to") //
	), //

	mail_password( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("password") //
			.setDescription("The password with which to authenticate to the SMTP host") //
	), //

	mail_port( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("port") //
			.setValueFilter(new IntegerValueFilter(1, 65535)) //
			.setDefault("25") //
			.setDescription("The port at which the mail-host is listening") //
	), //

	mail_ssl( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("ssl-mode") //
			.setValueFilter(new EnumValueFilter<>(false, SslMode.class)) //
			.setDefault(SslMode.NONE.name()) //
			.setDescription("The SSL mode to use when connecting to the server") //
	), //

	mail_to( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setValueSep(',') //
			.setArgumentName("email") //
			.setValueFilter(CLIFilters.EMAIL_FILTER) //
			.setDescription("Recipients for the status e-mail") //
	), //

	mail_user( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("user") //
			.setDescription("The user with which to authenticate to the SMTP host") //
	), //

	manifest_outcomes_export( //
		new OptionImpl() //
			.setLongOpt("manifest-outcomes") //
			.setArgumentLimits(1, -1) //
			.setValueSep(',') //
			.setArgumentName("export-outcome") //
			.setValueFilter(new EnumValueFilter<>(false, ExportResult.class)) //
			.setDescription("The export outcomes to include in the manifest (not specified = all outcomes)") //
	), //

	manifest_outcomes_import( //
		new OptionImpl() //
			.setLongOpt("manifest-outcomes") //
			.setArgumentLimits(1, -1) //
			.setValueSep(',') //
			.setArgumentName("import-outcome") //
			.setValueFilter(new EnumValueFilter<>(false, ImportResult.class)) //
			.setDescription("The import outcomes to include in the manifest (not specified = all outcomes)") //
	), //

	manifest_types( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setValueSep(',') //
			.setArgumentName("type") //
			.setValueFilter(new EnumValueFilter<>(false, CmfType.class)) //
			.setDescription("The object types to include in the manifest (not specified = all types)") //
	), //

	no_filename_map( //
		new OptionImpl() //
			.setDescription("Disable the use of the filename map (even if the default map exists)") //
	), //

	no_renditions( //
		new OptionImpl() //
			.setDescription("Disable renditions processing") //
	), //

	no_versions( //
		new OptionImpl() //
			.setDescription("Only operate on the objects' current versions") //
	), //

	non_recursive( //
		new OptionImpl() //
			.setDescription("Turn off counter recursion (i.e. to count a single folder without descending)") //
	), //

	organizer( //
		new OptionImpl() //
			.setShortOpt('o') //
			.setArgumentLimits(1) //
			.setArgumentName("organizer-name") //
			.setDescription(
				"The name of the content organizer to use in the Content directory (specific engines may override with their own defaults if they require it)") //
	), //

	password( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("password") //
			.setDescription("The password to authenticate with") //
	), //

	restrict_to( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("restriction-spec") //
			.setValueSep(',') //
			.setDescription(
				"Either a comma-separated list of ObjectRefs (TYPE:ID), or the path/url of a text file that contains them (one per line), which will be used to restrict which objects are imported") //
	), //

	role_map( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("role-map-file") //
			.setDefault("rolemap.xml") //
			.setDescription("The Properties (XML) file that contains the role name mappings to apply") //
	), //

	server( //
		new OptionImpl() //
			.setRequired(true) //
			.setArgumentLimits(1) //
			.setArgumentName("server-connection-info") //
			.setDescription("The server information for the connection") //
	), //

	skip_content( //
		new OptionImpl() //
			.setDescription("Don't process the actual content streams") //
	), //

	streams( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("stream-directory-or-config") //
			.setDescription(
				"The directory into which the content streams will be stored (if omitted, it will be placed in the 'content' subdirectory of the Database directory), or the XML file that describes the store configuration") //
	), //

	target( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("target-path") //
			.setDescription("The path location into which to import the contents") //
	), //

	threads( //
		ThreadsLaunchHelper.THREADS //
	), //

	transformations( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("transformations-file") //
			.setDescription("The object transformations descriptor file") //
	), //

	trim_path( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("number") //
			.setDescription("The number of leading path components to trim from the content being imported") //
	), //

	user( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("user") //
			.setDescription("The user to authenticate as") //
	), //

	user_map( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("user-map-file") //
			.setDefault("usermap.xml") //
			.setDescription("The Properties (XML) file that contains the user name mappings to apply") //
	), //

	validate_requirements( //
		new OptionImpl() //
			.setDescription(
				"Activate the validation of an object's requirements' import success during object import (object is skipped if any of its requirements fails to import properly)") //
	), //

	//
	;

	public final OptionImpl option;

	private CLIParam(Option option) {
		this(new OptionImpl(option));
	}

	private CLIParam(OptionImpl option) {
		if (option.getLongOpt() == null) {
			option.setLongOpt(name().replace('_', '-'));
		}
		this.option = option;
	}

	@Override
	public Option getOption() {
		return this.option;
	}

	public OptionImpl getOptionImpl() {
		return this.option;
	}
}