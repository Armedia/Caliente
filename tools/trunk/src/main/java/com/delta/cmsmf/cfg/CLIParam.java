package com.delta.cmsmf.cfg;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public enum CLIParam {
	//
	help(null, 0, "This help message"),
	cfg(null, 1, "The configuration file to use"),
	dfc(null, 1, "The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)"),
	dfc_prop(null, 1, "The dfc.properties file to use instead of the default"),
	dctm(null, 1, "The user's local Documentum path (i.e. instead of DOCUMENTUM)"),
	mode(null, 1, true, "The mode of operation, either 'import', 'export', 'encrypt', 'decrypt' or 'counter'"),
	engine(null, 1, true, "The engine to use for the operation (either dctm or shpt)"),
	server(null, 1, "The server URL to connect to (Documentum docbase spec, or the sharepoint server URL)"),
	repository(null, 1, "The repostory name to connect to within the target server (optional)"),
	user(null, 1, "The username to connect with"),
	password(null, 1, "The password to connect with"),
	domain(null, 1, "The domain the user belongs to"),
	log_name(null, 1, "The base name of the log file to use instead of the default (cmsmf-${action})"),
	log4j(null, 1, "The Log4j configuration (XML format) to use instead of the default (overrides --log_name)"),
	threads(Setting.THREADS, 1, "The number of threads to use while importing or exporting"),
	non_recursive(null, 0, "Turn off counter recursion (i.e. to count a single folder without descending)"),
	count_empty(null, 0, "Enable reporting of empty folders (i.e. folders with 0 non-folder children)"),
	count_path(null, 1, "The path within which to count objects for"),
	cmf_exclude_types(
		Setting.CMF_EXCLUDE_TYPES,
		-1,
		"The list of object types to be ignored during the operation (comma-separated)"),
	cmf_import_target_location(
		Setting.CMF_IMPORT_TARGET_LOCATION,
		1,
		"The path location into which to import the contents"),
	cmf_import_trim_prefix(
		Setting.CMF_IMPORT_TRIM_PREFIX,
		1,
		"The number of leading path components to trim from the content being imported"),
	special_users(
		Setting.SPECIAL_USERS,
		-1,
		"The special users that should not be imported into the target instance (comma-separated)"),
	special_groups(
		Setting.SPECIAL_GROUPS,
		-1,
		"The special users that should not be imported into the target instance (comma-separated)"),
	special_types(
		Setting.SPECIAL_TYPES,
		-1,
		"The special types that should not be imported into the target instance (comma-separated)"),
	batch_size(Setting.EXPORT_BATCH_SIZE, 1, "The batch size to use when exporting objects from Documentum"),
	post_process(Setting.POST_PROCESS_IMPORT, 0, "Whether to post-process the imported content"),
	source(
		Setting.EXPORT_PREDICATE,
		1,
		"The DQL 'from-where' predicate, or the name of the Sharepoint site, to use for exporting"),
	shpt_source_prefix(
		Setting.SHPT_SOURCE_PREFIX,
		1,
		"The prefix to pre-pend to Sharepoint source paths (i.e. /sites is the default)"),
	db(Setting.DB_DIRECTORY, 1, "The Database directory to use"),
	db_type(Setting.DB_TYPE, 1, "The Database type to use"),
	content(
		Setting.CONTENT_DIRECTORY,
		1,
		"The Content directory to use (if omitted, it will be placed in the 'content' subdirectory of the Database directory)"),
	content_strategy(
		Setting.CONTENT_ORGANIZATION,
		1,
		"The name of the organization strategy to use in the Content directory (specific engines may use their own defaults)"),
	attributes(Setting.OWNER_ATTRIBUTES, 1, "The attributes to check for"),
	errorCount(Setting.IMPORT_MAX_ERRORS, 1, "The number of errors to accept before aborting an import"),
	default_password(
		Setting.DEFAULT_USER_PASSWORD,
		1,
		"The default password to use for users being copied over (the default is to use the same login name)"),
	manifest_types(Setting.MANIFEST_TYPES, 1, "The object types to include in the manifest (ALL = all types)"),
	manifest_outcomes(Setting.MANIFEST_OUTCOMES, 1, "The outcomes to include in the manifest (ALL = all outcomes)"),
	mail_to(Setting.MAIL_RECIPIENTS, 1, "The comma-separated list of recipients for the status e-mails"),
	mail_from(Setting.MAIL_FROM_ADDX, 1, "The e-mail address to use as the sender for the status e-mails"),
	smtp_host(Setting.MAIL_SMTP_HOST, 1, "The SMTP server through which e-mail must be sent"),
	smtp_port(Setting.MAIL_SMTP_HOST, 1, "The port SMTP server is listening on"),
	skip_users(Setting.SKIP_USERS, 0, "Skip exporting users"),
	skip_groups(Setting.SKIP_GROUPS, 0, "Skip exporting groups"),
	skip_acls(Setting.SKIP_ACLS, 0, "Skip exporting acls"),
	skip_content(null, 0, "Skip importing document contents (only create \"empty\" documents)"),
	direct_fs(null, 0, "Export files to local FS duplicating the CMS's path"),
	no_renditions(
		null,
		0,
		"Only include the main content stream for documents (and only the first page where multipage is supported)"),
	no_versions(null, 0, "Only include the latest (current) version of documents"),
	job_name(null, 1, "The name of the synchronization job this export is to define"),
	reset_job(null, 0, "Reset any existing synchronization job settings with this export's"),
	content_model(null, -1, "The list of XML files that make up the Alfresco content model to use on import"),
	user_map(Setting.USER_MAP, 1, "The Properties (XML) file that contains the group name mappings to apply"),
	group_map(Setting.GROUP_MAP, 1, "The Properties (XML) file that contains the user name mappings to apply"),
	role_map(Setting.ROLE_MAP, 1, "The Properties (XML) file that contains the role name mappings to apply"),
	type_map(Setting.TYPE_MAP, 1, "The Properties (XML) file that contains the type mappings to apply"),
	no_dedup(null, 0, "Disable the automatic filename deduplication code"),
	no_name_fix(null, 0, "Disable the automatic filename fixing code"),
	filename_map(Setting.FILENAME_MAP, 1, "The filename map to be used for static file renames"),
	//
	;

	public final Setting property;
	public final Option option;

	private CLIParam(Setting property, int paramCount, boolean required, String description) {
		this.property = property;
		if (required) {
			OptionBuilder.isRequired();
		}
		OptionBuilder.withLongOpt(name().replace('_', '-'));
		OptionBuilder.withDescription(description);
		OptionBuilder.withValueSeparator(',');
		if (paramCount < 0) {
			OptionBuilder.hasArgs(Integer.MAX_VALUE);
		} else if (paramCount > 0) {
			OptionBuilder.hasArgs(paramCount);
		}
		this.option = OptionBuilder.create();
	}

	private CLIParam(Setting property, int paramCount, String description) {
		this(property, paramCount, false, description);
	}

	public boolean isPresent() {
		return CLIParam.isPresent(this);
	}

	public Boolean getBoolean() {
		String s = getString();
		return (s != null ? Boolean.valueOf(s) : null);
	}

	public boolean getBoolean(boolean def) {
		Boolean v = getBoolean();
		return (v != null ? v.booleanValue() : def);
	}

	public Integer getInteger() {
		String s = getString();
		return (s != null ? Integer.valueOf(s) : null);
	}

	public int getInteger(int def) {
		Integer v = getInteger();
		return (v != null ? v.intValue() : def);
	}

	public Double getDouble() {
		String s = getString();
		return (s != null ? Double.valueOf(s) : null);
	}

	public double getDouble(double def) {
		Double v = getDouble();
		return (v != null ? v.doubleValue() : def);
	}

	public String getString() {
		return CLIParam.getString(this);
	}

	public String getString(String def) {
		final String v = getString();
		return (v != null ? v : def);
	}

	private static final String[] NO_OPTS = new String[0];
	private static final Map<CLIParam, String> NO_PARSED = Collections.emptyMap();
	private static AtomicReference<Map<CLIParam, String>> CLI_PARSED = new AtomicReference<Map<CLIParam, String>>(
		CLIParam.NO_PARSED);

	public static String getString(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, String> m = CLIParam.getParsed();
		if (m == null) { return null; }
		return m.get(param);
	}

	public static boolean isPresent(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, String> m = CLIParam.getParsed();
		if (m == null) { return false; }
		return m.containsKey(param);
	}

	public static Map<CLIParam, String> getParsed() {
		return CLIParam.CLI_PARSED.get();
	}

	public static synchronized boolean parse(String... args) {
		if (args == null) {
			args = CLIParam.NO_OPTS;
		}
		// To start off, parse the command line
		Options options = new Options();
		for (CLIParam p : CLIParam.values()) {
			options.addOption(p.option);
		}

		CommandLineParser parser = new GnuParser();
		final CommandLine cli;
		try {
			cli = parser.parse(options, args);
		} catch (ParseException e) {
			new HelpFormatter().printHelp("CMSMF",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options,
				String.format("%nERROR: %s%n%n", e.getMessage()), true);
			return false;
		}

		if (cli.hasOption(CLIParam.help.option.getLongOpt())) {
			new HelpFormatter().printHelp("CMSMF",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options, null, true);
			return false;
		}

		// Convert the command-line parameters into "configuration properties"
		Map<CLIParam, String> cliParams = new EnumMap<CLIParam, String>(CLIParam.class);
		StringBuilder b = new StringBuilder();
		for (CLIParam p : CLIParam.values()) {
			if (cli.hasOption(p.option.getLongOpt())) {
				String[] v = cli.getOptionValues(p.option.getLongOpt());
				if ((v != null) && (v.length > 0)) {
					if (v.length == 1) {
						// Single value, life is easy :)
						cliParams.put(p, v[0]);
					} else {
						// Multi-value, must concatenate as comma-separated
						b.setLength(0);
						for (String s : v) {
							if (b.length() > 0) {
								b.append(',');
							}
							b.append(s);
						}
						cliParams.put(p, b.toString());
					}
				} else {
					cliParams.put(p, null);
				}
			}
		}
		CLIParam.CLI_PARSED.set(Collections.unmodifiableMap(cliParams));
		return true;
	}
}