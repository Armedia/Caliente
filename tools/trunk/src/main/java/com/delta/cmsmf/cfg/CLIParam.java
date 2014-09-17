package com.delta.cmsmf.cfg;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.armedia.commons.utilities.Tools;

public enum CLIParam {
	//
	help(null, false, "This help message"),
	cfg(null, true, "The configuration file to use"),
	dfc(null, true, "The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)"),
	dctm(null, true, "The user's local Documentum path (i.e. instead of DOCUMENTUM)"),
	mode(null, true, true, "The mode of operation, either 'encrypt', 'decrypt', 'import' or 'export'"),
	docbase(null, true, "The docbase name to connect to"),
	user(null, true, "The username to connect with"),
	password(null, true, "The password to connect with"),
	log_name(null, true, "The base name of the log file to use instead of the default (cmsmf-${action})"),
	log4j(null, true, "The Log4j configuration (XML format) to use instead of the default (overrides --log_name)"),
	threads(null, true, "The number of threads to use while importing or exporting"),
	special_users(Setting.SPECIAL_USERS, true,
		"The special users that should not be imported into the target instance (comma-separated)"),
		special_groups(Setting.SPECIAL_GROUPS, true,
			"The special users that should not be imported into the target instance (comma-separated)"),
			special_types(Setting.SPECIAL_TYPES, true,
				"The special types that should not be imported into the target instance (comma-separated)"),
				batch_size(Setting.EXPORT_BATCH_SIZE, true, "The batch size to use when exporting objects from Documentum"),
				post_process(Setting.POST_PROCESS_IMPORT, false, "Whether to post-process the imported content"),
				predicate(Setting.EXPORT_PREDICATE, true, "The DQL 'from-where' predicate to use for exporting"),
				db(Setting.DB_DIRECTORY, true, "The Database directory to use"),
				content(Setting.CONTENT_DIRECTORY, true, "The Content directory to use"),
				attributes(Setting.OWNER_ATTRIBUTES, true, "The attributes to check for"),
				errorCount(Setting.IMPORT_MAX_ERRORS, true, "The number of errors to accept before aborting an import"),
				defaultPassword(Setting.DEFAULT_USER_PASSWORD, true,
					"The default password to use for users being copied over (leave blank to use the same login name)"),
					mailTo(Setting.MAIL_RECIPIENTS, true, "The comma-separated list of recipients for the status e-mails"),
					mailFrom(Setting.MAIL_FROM_ADDX, true, "The e-mail address to use as the sender for the status e-mails"),
					smtpHost(Setting.MAIL_SMTP_HOST, true, "The SMTP server through which e-mail must be sent"),
					skip_users(Setting.SKIP_USERS, false, "Skip exporting users"),
					skip_groups(Setting.SKIP_GROUPS, false, "Skip exporting groups"),
					skip_acls(Setting.SKIP_ACLS, false, "Skip exporting acls");

	public final Setting property;
	public final Option option;

	private CLIParam(Setting property, boolean hasParameter, boolean required, String description) {
		this.property = property;
		this.option = new Option(null, name().replace('_', '-'), hasParameter, description);
		this.option.setRequired(required);
	}

	private CLIParam(Setting property, boolean hasParameter, String description) {
		this(property, hasParameter, false, description);
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
		String v = getString();
		return Tools.coalesce(v, def);
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

		CommandLineParser parser = new PosixParser();
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
		for (CLIParam p : CLIParam.values()) {
			if (cli.hasOption(p.option.getLongOpt())) {
				cliParams.put(p, cli.getOptionValue(p.option.getLongOpt()));
			}
		}
		CLIParam.CLI_PARSED.set(Collections.unmodifiableMap(cliParams));
		return true;
	}
}