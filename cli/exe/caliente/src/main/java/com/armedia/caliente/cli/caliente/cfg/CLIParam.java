package com.armedia.caliente.cli.caliente.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.armedia.caliente.cli.caliente.launcher.CalienteLauncher;
import com.armedia.commons.utilities.Tools;

public enum CLIParam {
	//
	help(null, 0, "This help message"),
	cfg(null, 1, "The configuration file to use"),
	dfc(null, 1, "The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)"),
	dfc_prop(null, 1, "The dfc.properties file to use instead of the default"),
	dctm(null, 1, "The user's local Documentum path (i.e. instead of DOCUMENTUM)"),
	lib(
		null,
		1,
		"The directory which contains extra classes (JARs, ZIPs or a classes directory) that should be added to the classpath"),
	mode(null, 1, true, "The mode of operation, either 'import', 'export', 'encrypt', 'decrypt' or 'counter'"),
	engine(null, 1, true, "The engine to use for the operation (either dctm or shpt)"),
	server(null, 1, "The server URL to connect to (Documentum docbase spec, or the sharepoint server URL)"),
	repository(null, 1, "The repostory name to connect to within the target server (optional)"),
	user(null, 1, "The username to connect with"),
	password(null, 1, "The password to connect with"),
	domain(null, 1, "The domain the user belongs to"),
	log(
		null,
		1,
		String.format(
			"The base name of the log file to use. The default is %s - you can use a similar format with similar placeholders (make sure you escape the $ sign if necessary!)",
			CalienteLauncher.DEFAULT_LOG_FORMAT)),
	log_cfg(null, 1, "The Log4j configuration (XML format) to use instead of the default (overrides --log)"),
	threads(Setting.THREADS, 1, "The number of threads to use while importing or exporting"),
	non_recursive(null, 0, "Turn off counter recursion (i.e. to count a single folder without descending)"),
	count_empty(null, 0, "Enable reporting of empty folders (i.e. folders with 0 non-folder children)"),
	count_private(
		null,
		1,
		"For any cabinets encountered, evaluate their private status and include (true/yes/1), exclude (false/no/0) the private ones. The special value 'any' (default) causes the status to not be taken into account"),
	count_hidden(
		null,
		1,
		"For any folders encountered, evaluate their hidden status and include (true/yes/1), exclude (false/no/0) the hidden ones. The special value 'any' (default) causes the status to not be taken into account"),
	count_include(
		null,
		-1,
		"Include the folder in the count (defaults to only these, may be specified multiple times) - path or object ID is valid"),
	count_exclude(
		null,
		-1,
		"Exclude the folder in the count (defaults to ALL except these, may be specified multiple times) - path or object ID is valid"),
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
	content(
		Setting.CONTENT_DIRECTORY,
		1,
		"The Content directory to use (if omitted, it will be placed in the 'content' subdirectory of the Database directory)"),
	content_strategy(
		Setting.CONTENT_ORGANIZATION,
		1,
		"The name of the organization strategy to use in the Content directory (specific engines may use their own defaults)"),
	owner_attributes(Setting.OWNER_ATTRIBUTES, 1, "The owner_attributes to check for"),
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
	no_filename_map(null, 0, "Disable the use of the filename map (even if the default map exists)"),
	validate_requirements(
		null,
		0,
		"Activate the validation of an object's requirements' import success during object import (object is skipped if any of its requirements fails to import properly)"),
	filename_map(
		Setting.FILENAME_MAP,
		1,
		"The Properties (XML) file that contains the static filename mappings to be applied"),
	object_store_config(null, 1, "The properties file to use for Object Store DB configuration"),
	content_store_config(null, 1, "The properties file to use for Content Store DB configuration"),
	copy_content(null, 0, "Enable the copying of content for the Local engine"),
	ignore_empty_folders(null, 0, "Enable the copying of content for the Local engine"),
	transformations(null, 1, "The object transformations descriptor file"),
	filters(null, 1, "The object filters descriptor file"),
	external_metadata(null, 1, "The external metadata descriptor file"),
	//
	;

	public final Setting property;
	public final Option option;
	private final int paramCount;

	private CLIParam(Setting property, int paramCount, boolean required, String description) {
		String longOpt = name().replace('_', '-');
		Builder b = Option.builder();
		if (required) {
			b.required();
		}
		b.longOpt(longOpt);
		b.desc(description);
		b.valueSeparator(',');
		if (paramCount < 0) {
			b.hasArgs();
		} else if (paramCount > 0) {
			b.numberOfArgs(paramCount);
		}
		this.option = b.build();
		this.paramCount = paramCount;
		this.property = property;
	}

	private CLIParam(Setting property, int paramCount, String description) {
		this(property, paramCount, false, description);
	}

	public boolean isPresent() {
		return CLIParam.isPresent(this);
	}

	public Boolean getBoolean() {
		String s = getString();
		return (s != null ? Tools.toBoolean(s) : null);
	}

	public boolean getBoolean(boolean def) {
		Boolean v = getBoolean();
		return (v != null ? v.booleanValue() : def);
	}

	public List<Boolean> getAllBoolean() {
		List<String> l = getAllString();
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	public Integer getInteger() {
		String s = getString();
		return (s != null ? Integer.valueOf(s) : null);
	}

	public int getInteger(int def) {
		Integer v = getInteger();
		return (v != null ? v.intValue() : def);
	}

	public List<Integer> getAllInteger() {
		List<String> l = getAllString();
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	public Double getDouble() {
		String s = getString();
		return (s != null ? Double.valueOf(s) : null);
	}

	public double getDouble(double def) {
		Double v = getDouble();
		return (v != null ? v.doubleValue() : def);
	}

	public List<Double> getAllDouble() {
		List<String> l = getAllString();
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Double> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Double.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	public String getString() {
		return CLIParam.getString(this);
	}

	public String getString(String def) {
		final String v = getString();
		return (v != null ? v : def);
	}

	public List<String> getAllString() {
		return CLIParam.getAllString(this);
	}

	private static final String[] NO_OPTS = new String[0];
	private static final Map<CLIParam, List<String>> NO_PARSED = Collections.emptyMap();
	private static final List<String> NO_REMAINING = Collections.emptyList();
	private static AtomicReference<Map<CLIParam, List<String>>> CLI_PARSED = new AtomicReference<>(CLIParam.NO_PARSED);
	private static AtomicReference<List<String>> CLI_REMAINING = new AtomicReference<>(CLIParam.NO_REMAINING);

	public static String getString(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, List<String>> m = CLIParam.getParsed();
		if (m == null) { return null; }
		List<String> l = m.get(param);
		if ((l == null) || l.isEmpty()) { return null; }
		return l.get(0);
	}

	public static List<String> getAllString(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, List<String>> m = CLIParam.getParsed();
		if (m == null) { return null; }
		List<String> l = m.get(param);
		if ((l == null) || l.isEmpty()) { return CLIParam.NO_REMAINING; }
		return l;
	}

	public static boolean isPresent(CLIParam param) {
		if (param == null) { throw new IllegalArgumentException("Must provide a parameter to search for"); }
		Map<CLIParam, List<String>> m = CLIParam.getParsed();
		if (m == null) { return false; }
		return m.containsKey(param);
	}

	public static Map<CLIParam, List<String>> getParsed() {
		return CLIParam.CLI_PARSED.get();
	}

	public static List<String> getRemaining() {
		return CLIParam.CLI_REMAINING.get();
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

		CommandLineParser parser = new DefaultParser();
		final CommandLine cli;
		try {
			cli = parser.parse(options, args);
		} catch (ParseException e) {
			new HelpFormatter().printHelp("Caliente",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options,
				String.format("%nERROR: %s%n%n", e.getMessage()), true);
			return false;
		}

		if (cli.hasOption(CLIParam.help.option.getLongOpt())) {
			new HelpFormatter().printHelp("Caliente",
				String.format("%nAvailable Parameters:%n------------------------------%n"), options, null, true);
			return false;
		}

		// Convert the command-line parameters into "configuration properties"
		Map<CLIParam, List<String>> cliParams = new EnumMap<>(CLIParam.class);
		for (CLIParam p : CLIParam.values()) {
			if (cli.hasOption(p.option.getLongOpt())) {
				// If it takes no parameters, ignore whatever was submitted
				if (p.paramCount == 0) {
					cliParams.put(p, CLIParam.NO_REMAINING);
					continue;
				}

				// It takes parameters, so ... store them
				String[] v = cli.getOptionValues(p.option.getLongOpt());
				if ((v != null) && (v.length > 0)) {
					List<String> l = null;
					// If it only has one, or it only takes one, only keep one
					if ((v.length == 1) || (p.paramCount == 1)) {
						// Single value, life is easy :)
						l = Collections.singletonList(v[0]);
					} else {
						l = Arrays.asList(v);
					}
					cliParams.put(p, Tools.freezeList(l));
				} else {
					// The parameters may be optional....???
					cliParams.put(p, CLIParam.NO_REMAINING);
				}
			}
		}
		List<?> remaining = cli.getArgList();
		if (!remaining.isEmpty()) {
			List<String> l = new ArrayList<>(remaining.size());
			for (Object o : remaining) {
				l.add(Tools.toString(o));
			}
			CLIParam.CLI_REMAINING.set(Tools.freezeList(l));
		}
		CLIParam.CLI_PARSED.set(Tools.freezeMap(cliParams));
		return true;
	}
}
